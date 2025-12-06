"""
VAE (Variational Autoencoder) 모델
학습 패턴을 인코딩하고 새로운 학습 계획을 생성
"""

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np

class VAEModel:
    def __init__(self, input_dim=128, latent_dim=16):
        """
        Args:
            input_dim: 입력 특징 차원
            latent_dim: 잠재 공간 차원
        """
        self.input_dim = input_dim
        self.latent_dim = latent_dim
        self.model = self._build_vae()
        
    def _build_encoder(self):
        """인코더 네트워크"""
        inputs = keras.Input(shape=(self.input_dim,))
        x = layers.Dense(64, activation='relu')(inputs)
        x = layers.BatchNormalization()(x)
        x = layers.Dropout(0.2)(x)
        x = layers.Dense(32, activation='relu')(x)
        
        z_mean = layers.Dense(self.latent_dim, name='z_mean')(x)
        z_log_var = layers.Dense(self.latent_dim, name='z_log_var')(x)
        
        return keras.Model(inputs, [z_mean, z_log_var], name='encoder')
    
    def _build_decoder(self):
        """디코더 네트워크"""
        latent_inputs = keras.Input(shape=(self.latent_dim,))
        x = layers.Dense(32, activation='relu')(latent_inputs)
        x = layers.BatchNormalization()(x)
        x = layers.Dense(64, activation='relu')(x)
        outputs = layers.Dense(self.input_dim, activation='sigmoid')(x)
        
        return keras.Model(latent_inputs, outputs, name='decoder')
    
    class Sampling(layers.Layer):
        """재매개변수화 트릭을 사용한 샘플링"""
        def call(self, inputs):
            z_mean, z_log_var = inputs
            batch = tf.shape(z_mean)[0]
            dim = tf.shape(z_mean)[1]
            epsilon = tf.random.normal(shape=(batch, dim))
            return z_mean + tf.exp(0.5 * z_log_var) * epsilon
    
    def _build_vae(self):
        """VAE 모델 구성"""
        encoder = self._build_encoder()
        decoder = self._build_decoder()
        
        inputs = keras.Input(shape=(self.input_dim,))
        z_mean, z_log_var = encoder(inputs)
        z = self.Sampling()([z_mean, z_log_var])
        reconstructed = decoder(z)
        
        vae = keras.Model(inputs, reconstructed, name='vae')
        
        # VAE 손실 함수
        reconstruction_loss = keras.losses.binary_crossentropy(inputs, reconstructed)
        reconstruction_loss *= self.input_dim
        
        kl_loss = -0.5 * tf.reduce_sum(
            1 + z_log_var - tf.square(z_mean) - tf.exp(z_log_var),
            axis=1
        )
        
        vae_loss = tf.reduce_mean(reconstruction_loss + kl_loss)
        vae.add_loss(vae_loss)
        
        return vae
    
    def compile_model(self, learning_rate=0.001):
        """모델 컴파일"""
        self.model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=learning_rate)
        )
    
    def train(self, train_data, epochs=50, batch_size=32, validation_split=0.2):
        """
        모델 학습
        
        Args:
            train_data: 학습 데이터 (학생들의 학습 패턴 특징 벡터)
            epochs: 학습 에폭 수
            batch_size: 배치 크기
            validation_split: 검증 데이터 비율
        """
        history = self.model.fit(
            train_data, train_data,
            epochs=epochs,
            batch_size=batch_size,
            validation_split=validation_split,
            verbose=1
        )
        return history
    
    def generate_plan(self, reference_patterns, num_samples=1):
        """
        기준 패턴을 기반으로 새로운 학습 계획 생성
        
        Args:
            reference_patterns: 참고할 학습 패턴 (1등급 학생들의 패턴)
            num_samples: 생성할 샘플 수
        
        Returns:
            생성된 학습 계획 특징 벡터
        """
        encoder = self.model.get_layer('encoder')
        decoder = self.model.get_layer('decoder')
        
        z_mean, z_log_var = encoder.predict(reference_patterns)
        z = z_mean + tf.random.normal(z_mean.shape) * tf.exp(0.5 * z_log_var)
        
        generated = decoder.predict(z)
        return generated
    
    def save_model(self, filepath):
        """모델 저장"""
        self.model.save(filepath)
    
    def load_model(self, filepath):
        """모델 로드"""
        self.model = keras.models.load_model(filepath)


def preprocess_study_data(study_data):
    """
    학습 데이터 전처리
    
    Args:
        study_data: 학습 기록 데이터프레임
        컬럼: start_time, end_time, subject, activity_type, difficulty, estimated_time, actual_time
    
    Returns:
        특징 벡터 (numpy array)
    """
    features = []
    
    for _, row in study_data.iterrows():
        # 시간 관련 특징
        hour = row['start_time'].hour
        duration = row['actual_time']
        efficiency = row['estimated_time'] / max(row['actual_time'], 1)
        
        # 과목 원핫 인코딩 (6개 과목)
        subject_map = {'국어': 0, '수학': 1, '영어': 2, '역사': 3, '통합과학': 4, '사회': 5}
        subject_vec = [0] * 6
        if row['subject'] in subject_map:
            subject_vec[subject_map[row['subject']]] = 1
        
        # 활동 유형 원핫 인코딩
        activity_map = {'문제풀이': 0, '개념학습': 1, '복습': 2, '모의고사': 3}
        activity_vec = [0] * 4
        if row['activity_type'] in activity_map:
            activity_vec[activity_map[row['activity_type']]] = 1
        
        # 난이도 원핫 인코딩
        difficulty_map = {'쉬움': 0, '보통': 1, '어려움': 2}
        difficulty_vec = [0] * 3
        if row['difficulty'] in difficulty_map:
            difficulty_vec[difficulty_map[row['difficulty']]] = 1
        
        # 특징 결합
        feature = [hour/24, duration/120, efficiency] + subject_vec + activity_vec + difficulty_vec
        
        # 나머지를 0으로 패딩하여 128차원 맞춤
        feature += [0] * (128 - len(feature))
        features.append(feature)
    
    return np.array(features, dtype=np.float32)


def create_study_plan_from_vae(vae_model, top_student_data, target_grade=3, adjustment_factor=1.2):
    """
    VAE를 사용하여 학습 계획 생성
    
    Args:
        vae_model: 학습된 VAE 모델
        top_student_data: 상위권 학생들의 학습 데이터
        target_grade: 목표 등급
        adjustment_factor: 학습량 조정 계수
    
    Returns:
        생성된 학습 계획
    """
    reference_features = preprocess_study_data(top_student_data)
    generated_features = vae_model.generate_plan(reference_features, num_samples=1)
    
    # 하위 등급 학생을 위한 조정
    if target_grade > 2:
        generated_features *= adjustment_factor
    
    return generated_features


if __name__ == '__main__':
    # 예제 사용법
    print("VAE 모델 초기화...")
    vae = VAEModel(input_dim=128, latent_dim=16)
    vae.compile_model(learning_rate=0.001)
    
    # 더미 데이터로 테스트
    dummy_data = np.random.rand(1000, 128).astype(np.float32)
    
    print("모델 학습 시작...")
    history = vae.train(dummy_data, epochs=10, batch_size=32)
    
    print("모델 저장...")
    vae.save_model('vae_study_planner.h5')
    
    print("학습 계획 생성 테스트...")
    test_pattern = np.random.rand(5, 128).astype(np.float32)
    generated_plan = vae.generate_plan(test_pattern)
    print(f"생성된 계획 shape: {generated_plan.shape}")
    
    print("VAE 모델 준비 완료!")
