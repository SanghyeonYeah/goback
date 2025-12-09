const rateLimit = require('express-rate-limit');
const { body, query, param, validationResult } = require('express-validator');

// SQL Injection 방어 - 입력 검증 규칙
const sanitizeInput = (input) => {
  if (typeof input !== 'string') return input;
  
  // 위험한 SQL 키워드 제거
  const sqlKeywords = [
    'DROP', 'DELETE', 'TRUNCATE', 'ALTER', 'EXEC', 'EXECUTE',
    '--', ';--', '/*', '*/', 'xp_', 'sp_', 'UNION', 'SELECT'
  ];
  
  let sanitized = input;
  sqlKeywords.forEach(keyword => {
    const regex = new RegExp(keyword, 'gi');
    sanitized = sanitized.replace(regex, '');
  });
  
  return sanitized;
};

// XSS 방어 - HTML 태그 이스케이프
const escapeHtml = (input) => {
  if (typeof input !== 'string') return input;
  
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '/': '&#x2F;'
  };
  
  return input.replace(/[&<>"'/]/g, (char) => map[char]);
};

// 입력 검증 결과 처리
const handleValidationErrors = (req, res, next) => {
  const errors = validationResult(req);
  
  if (!errors.isEmpty()) {
    return res.status(400).json({ 
      error: '입력 값이 올바르지 않습니다.',
      details: errors.array().map(err => ({
        field: err.param,
        message: err.msg
      }))
    });
  }
  
  next();
};

// Rate Limiting 설정
const createRateLimiter = (windowMs, max, message) => {
  return rateLimit({
    windowMs,
    max,
    message: { error: message },
    standardHeaders: true,
    legacyHeaders: false,
    handler: (req, res) => {
      console.log(`[Rate Limit] IP ${req.ip} - ${req.method} ${req.path}`);
      res.status(429).json({ 
        error: message,
        retryAfter: Math.ceil(windowMs / 1000)
      });
    }
  });
};

// 일반 API Rate Limiter (분당 100회)
const apiLimiter = createRateLimiter(
  60 * 1000, // 1분
  100,
  '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.'
);

// 인증 Rate Limiter (15분당 5회)
const authLimiter = createRateLimiter(
  15 * 60 * 1000, // 15분
  5,
  '로그인 시도가 너무 많습니다. 15분 후 다시 시도해주세요.'
);

// 회원가입 Rate Limiter (시간당 3회)
const registerLimiter = createRateLimiter(
  60 * 60 * 1000, // 1시간
  3,
  '회원가입 시도가 너무 많습니다. 1시간 후 다시 시도해주세요.'
);

// 문제 제출 Rate Limiter (분당 10회)
const problemSubmitLimiter = createRateLimiter(
  60 * 1000, // 1분
  10,
  '답안 제출이 너무 많습니다. 잠시 후 다시 시도해주세요.'
);

// CSRF 토큰 검증
const verifyCsrfToken = (req, res, next) => {
  // GET 요청은 CSRF 검증 제외
  if (req.method === 'GET' || req.method === 'HEAD' || req.method === 'OPTIONS') {
    return next();
  }

  const token = req.body._csrf || req.headers['csrf-token'] || req.headers['x-csrf-token'];
  
  if (!token || token !== req.csrfToken()) {
    console.log(`[CSRF 위반] IP ${req.ip} - ${req.method} ${req.path}`);
    return res.status(403).json({ 
      error: 'CSRF 토큰이 유효하지 않습니다. 페이지를 새로고침해주세요.' 
    });
  }
  
  next();
};

// Content Security Policy 헤더 설정
const setSecurityHeaders = (req, res, next) => {
  // CSP
  res.setHeader(
    'Content-Security-Policy',
    "default-src 'self'; " +
    "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
    "font-src 'self' https://fonts.gstatic.com; " +
    "img-src 'self' data: https:; " +
    "connect-src 'self'"
  );
  
  // X-Frame-Options
  res.setHeader('X-Frame-Options', 'DENY');
  
  // X-Content-Type-Options
  res.setHeader('X-Content-Type-Options', 'nosniff');
  
  // X-XSS-Protection
  res.setHeader('X-XSS-Protection', '1; mode=block');
  
  // Referrer-Policy
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  
  // Permissions-Policy
  res.setHeader(
    'Permissions-Policy',
    'geolocation=(), microphone=(), camera=()'
  );
  
  next();
};

// 파일 업로드 검증 (선택적 - 필요시 사용)
const validateFileUpload = (allowedTypes, maxSize) => {
  return (req, res, next) => {
    if (!req.file) {
      return next();
    }

    // 파일 타입 검증
    if (allowedTypes && !allowedTypes.includes(req.file.mimetype)) {
      return res.status(400).json({ 
        error: '허용되지 않는 파일 형식입니다.',
        allowedTypes
      });
    }

    // 파일 크기 검증
    if (maxSize && req.file.size > maxSize) {
      return res.status(400).json({ 
        error: `파일 크기는 ${maxSize / 1024 / 1024}MB를 초과할 수 없습니다.` 
      });
    }

    next();
  };
};

// IP 기반 차단 리스트 (선택적)
const blacklistedIPs = new Set();

const checkBlacklist = (req, res, next) => {
  const clientIP = req.ip || req.connection.remoteAddress;
  
  if (blacklistedIPs.has(clientIP)) {
    console.log(`[차단된 IP] ${clientIP} - ${req.method} ${req.path}`);
    return res.status(403).json({ error: '접근이 차단되었습니다.' });
  }
  
  next();
};

const addToBlacklist = (ip) => {
  blacklistedIPs.add(ip);
  console.log(`[IP 차단] ${ip} 추가됨`);
};

const removeFromBlacklist = (ip) => {
  blacklistedIPs.delete(ip);
  console.log(`[IP 차단 해제] ${ip} 제거됨`);
};

// 요청 크기 제한
const limitRequestSize = (maxSize = '10mb') => {
  return (req, res, next) => {
    const contentLength = req.headers['content-length'];
    
    if (contentLength && parseInt(contentLength) > parseInt(maxSize) * 1024 * 1024) {
      return res.status(413).json({ 
        error: `요청 크기가 너무 큽니다. 최대 ${maxSize}까지 허용됩니다.` 
      });
    }
    
    next();
  };
};

// SQL Injection 패턴 탐지
const detectSqlInjection = (req, res, next) => {
  const sqlPattern = /(\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION)\b)/gi;
  
  const checkValue = (value) => {
    if (typeof value === 'string' && sqlPattern.test(value)) {
      return true;
    }
    if (typeof value === 'object' && value !== null) {
      return Object.values(value).some(checkValue);
    }
    return false;
  };

  if (checkValue(req.body) || checkValue(req.query) || checkValue(req.params)) {
    console.log(`[SQL Injection 탐지] IP ${req.ip} - ${req.method} ${req.path}`);
    console.log('Body:', req.body);
    console.log('Query:', req.query);
    console.log('Params:', req.params);
    
    return res.status(400).json({ 
      error: '유효하지 않은 입력입니다.' 
    });
  }
  
  next();
};

// XSS 패턴 탐지
const detectXss = (req, res, next) => {
  const xssPattern = /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi;
  
  const checkValue = (value) => {
    if (typeof value === 'string' && xssPattern.test(value)) {
      return true;
    }
    if (typeof value === 'object' && value !== null) {
      return Object.values(value).some(checkValue);
    }
    return false;
  };

  if (checkValue(req.body) || checkValue(req.query)) {
    console.log(`[XSS 탐지] IP ${req.ip} - ${req.method} ${req.path}`);
    
    return res.status(400).json({ 
      error: '유효하지 않은 입력입니다.' 
    });
  }
  
  next();
};

// 입력 정제 미들웨어
const sanitizeInputs = (req, res, next) => {
  // Body 정제
  if (req.body && typeof req.body === 'object') {
    Object.keys(req.body).forEach(key => {
      if (typeof req.body[key] === 'string') {
        req.body[key] = req.body[key].trim();
      }
    });
  }
  
  // Query 정제
  if (req.query && typeof req.query === 'object') {
    Object.keys(req.query).forEach(key => {
      if (typeof req.query[key] === 'string') {
        req.query[key] = req.query[key].trim();
      }
    });
  }
  
  next();
};

// 보안 로깅
const securityLogger = (req, res, next) => {
  const startTime = Date.now();
  
  res.on('finish', () => {
    const duration = Date.now() - startTime;
    const log = {
      timestamp: new Date().toISOString(),
      method: req.method,
      path: req.path,
      ip: req.ip,
      userAgent: req.headers['user-agent'],
      statusCode: res.statusCode,
      duration: `${duration}ms`,
      userId: req.session?.user?.id || 'anonymous'
    };
    
    // 보안 관련 상태 코드 로깅
    if ([401, 403, 429].includes(res.statusCode)) {
      console.warn('[보안 경고]', JSON.stringify(log));
    }
  });
  
  next();
};

// 공통 입력 검증 규칙
const validationRules = {
  username: body('username')
    .trim()
    .isLength({ min: 3, max: 20 })
    .withMessage('아이디는 3-20자여야 합니다')
    .matches(/^[a-zA-Z0-9_]+$/)
    .withMessage('아이디는 영문, 숫자, 언더스코어만 사용 가능합니다'),
  
  email: body('email')
    .trim()
    .isEmail()
    .withMessage('올바른 이메일을 입력하세요')
    .normalizeEmail(),
  
  password: body('password')
    .isLength({ min: 8, max: 100 })
    .withMessage('비밀번호는 8-100자여야 합니다')
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
    .withMessage('비밀번호는 대소문자와 숫자를 포함해야 합니다'),
  
  studentId: body('studentId')
    .trim()
    .notEmpty()
    .withMessage('학번을 입력하세요')
    .isLength({ max: 20 })
    .withMessage('학번이 너무 깁니다'),
  
  id: param('id')
    .isInt({ min: 1 })
    .withMessage('유효하지 않은 ID입니다'),
  
  date: query('date')
    .optional()
    .isISO8601()
    .withMessage('올바른 날짜 형식이 아닙니다')
};

module.exports = {
  // Rate Limiters
  apiLimiter,
  authLimiter,
  registerLimiter,
  problemSubmitLimiter,
  createRateLimiter,
  
  // 입력 검증
  handleValidationErrors,
  validationRules,
  
  // 보안 검증
  verifyCsrfToken,
  setSecurityHeaders,
  detectSqlInjection,
  detectXss,
  sanitizeInputs,
  sanitizeInput,
  escapeHtml,
  
  // IP 관리
  checkBlacklist,
  addToBlacklist,
  removeFromBlacklist,
  
  // 기타
  validateFileUpload,
  limitRequestSize,
  securityLogger
};