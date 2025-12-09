const functions = require("firebase-functions");
const express = require("express");
const path = require("path");
const session = require("express-session");
const helmet = require("helmet");
const compression = require("compression");
const cookieParser = require("cookie-parser");
const csrf = require("csurf");
const rateLimit = require("express-rate-limit");
const morgan = require("morgan");
require("dotenv").config();

const app = express();

// Firebase 환경에서는 프록시 신뢰 필수 (secure cookie 때문)
app.set("trust proxy", 1);

// 라우트 임포트
const authRoutes = require("./routes/auth");
const todoRoutes = require("./routes/todo");
const problemRoutes = require("./routes/problem");
const rankingRoutes = require("./routes/ranking");
const pvpRoutes = require("./routes/pvp");
const adminRoutes = require("./routes/admin");
const userRoutes = require("./routes/user");

// 보안 미들웨어
app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        styleSrc: ["'self'", "'unsafe-inline'", "https://fonts.googleapis.com"],
        fontSrc: ["'self'", "https://fonts.gstatic.com"],
        scriptSrc: ["'self'", "'unsafe-inline'"],
        imgSrc: ["'self'", "data:", "https:"],
        formAction: ["'self'"],
      },
    },
  })
);

// 압축
app.use(compression());

// 로깅
app.use(morgan("combined"));

// Body parser
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true, limit: "10mb" }));
app.use(cookieParser());

// 정적 파일
app.use(express.static(path.join(__dirname, "public")));

// View engine
app.set("view engine", "ejs");
app.set("views", path.join(__dirname, "views"));

// Session 설정 (Firebase 대응)
app.use(
  session({
    secret: process.env.SESSION_SECRET || "firebase-secret",
    resave: false,
    saveUninitialized: false,
    cookie: {
      secure: process.env.NODE_ENV === "production",
      httpOnly: true,
      sameSite: "lax",
      maxAge: 24 * 60 * 60 * 1000, // 24시간
    },
  })
);

// CSRF 보호 (쿠키 기반)
const csrfProtection = csrf({ cookie: true });
app.use(csrfProtection);

app.use((req, res, next) => {
  res.locals.csrfToken = req.csrfToken();
  res.locals.user = req.session.user || null;
  next();
});

// Rate limiting (API)
app.use(
  "/api/",
  rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 100,
    message: "너무 많은 요청을 보냈습니다. 잠시 후 다시 시도해주세요.",
  })
);

// 로그인 시도 제한
app.use(
  "/auth/login",
  rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5,
    message: "로그인 시도가 너무 많습니다. 15분 후 다시 시도해주세요.",
  })
);

// Firebase에서는 HTTPS 리디렉션 / HTTPS 서버 생성 / app.listen 전부 필요 없음

// 라우트 설정
app.use("/auth", authRoutes);
app.use("/api/todo", todoRoutes);
app.use("/api/problem", problemRoutes);
app.use("/api/ranking", rankingRoutes);
app.use("/api/pvp", pvpRoutes);
app.use("/admin", adminRoutes);
app.use("/user", userRoutes);

// 메인 페이지
app.get("/", (req, res) => {
  if (!req.session.user) {
    return res.redirect("/auth/login");
  }
  res.redirect("/home");
});

app.get("/home", (req, res) => {
  if (!req.session.user) {
    return res.redirect("/auth/login");
  }

  res.render("home", {
    user: req.session.user,
    dday: 0,
    season: null,
    todos: {
      total: 0,
      completed: 0,
    },
    seasonRanking: [],
    dailyRanking: [],
    todayTodos: [],
  });
});

// Todo 페이지
app.get("/todo", async (req, res) => {
  try {
    if (!req.session.user) {
      return res.redirect("/auth/login");
    }

    const { pool } = require("./database/init");

    const userId = req.session.user.id;

    const result = await pool.query(
      `
      SELECT id, subject, task, completed 
      FROM todos 
      WHERE user_id = $1 AND date = CURRENT_DATE
      ORDER BY created_at ASC
    `,
      [userId]
    );

    res.render("todo", {
      todos: result.rows,
    });
  } catch (err) {
    console.error(err);
    res.render("todo", { todos: [] });
  }
});

// 달력 페이지
app.get("/calendar", (req, res) => {
  if (!req.session.user) {
    return res.redirect("/auth/login");
  }

  const now = new Date();
  const currentMonth = `${now.getFullYear()}-${now.getMonth() + 1}`;

  res.render("calendar", {
    user: req.session.user,
    currentMonth,
  });
});

// 랭킹 페이지
app.get("/ranking", (req, res) => {
  if (!req.session.user) {
    return res.redirect("/auth/login");
  }
  res.render("ranking", { user: req.session.user });
});

// 문제 풀이 페이지
app.get("/problem", (req, res) => {
  if (!req.session.user) {
    return res.redirect("/auth/login");
  }

  const stats = {
    totalSolved: 0,
    correctRate: 0,
    streak: 0,
  };

  res.render("problem", {
    user: req.session.user,
    stats,
  });
});

// PVP 페이지
app.get("/pvp", (req, res) => {
  if (!req.session.user) {
    return res.redirect("/auth/login");
  }

  const match = null;
  res.render("pvp", {
    user: req.session.user,
    match,
  });
});

// 404 에러
app.use((req, res) => {
  res.status(404).send("페이지를 찾을 수 없습니다.");
});

// 에러 핸들러
app.use((err, req, res, next) => {
  console.error(err.stack);

  if (err.code === "EBADCSRFTOKEN") {
    return res
      .status(403)
      .send("세션이 만료되었습니다. 페이지를 새로고침해주세요.");
  }

  res.status(500).send("서버 오류가 발생했습니다.");
});

// Firebase Functions로 최종 export
exports.app = functions.https.onRequest(app);
