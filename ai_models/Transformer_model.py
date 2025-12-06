"""
Transformer 모델
시계열 학습 데이터 분석 및 패턴 예측
"""

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np

class TransformerBlock(layers.Layer):
    """Transformer 블록"""
    def __init__(self, embed_dim, num_heads, ff_dim, rate=0.1):
        super(TransformerBlock, self).__init__()
        self.att = layers.MultiHeadAttention(num_heads=num_heads, key_dim=embed_dim)
        self.ffn = keras.Sequential([
            layers.Dense(ff_dim, activation="relu"),
            layers.Dense(embed_dim),
        ])
        self.layernorm1 = layers.LayerNormalization(epsilon=1e-6)
        self.layernorm2 = layers.LayerNormalization(epsilon=1e-6)
        self.dropout1 = layers.Dropout(rate)
        self.dropout2 = layers.Dropout(rate)

    def call(self, inputs, training):
        attn_output = self.att(inputs, inputs)
        attn_output = self.dropout1(attn_output, training=training)
        out1 = self.layernorm1(inputs + attn_output)
        ffn_output = self.ffn(out1)
        ffn_output = self.dropout2(ffn_output, training=training)
        return self.layernorm2(out1 + ffn_output)


class PositionalEncoding(layers.Layer):
    """위치 인코딩"""
    def __init__(self, max_len, embed_dim):
        super(PositionalEncoding, self).__init__()
        self.pos_encoding = self.positional_encoding(max_len, embed_dim)

    def get_angles(self, pos, i, embed_dim):
        angle_rates = 1 / np.power(10000, (2 * (i // 2)) / np.float32(embed_dim))
        return pos * angle_rates

    def positional_encoding(self, max_len, embed_dim):
        angle_rads = self.get_angles(
            np.arange(max_len)[:, np.newaxis],
            np.arange(embed_dim)[np.newaxis, :],
            embed_dim
        )
        angle_rads[:, 0::2] = np.sin(angle_rads[:, 0::2])
        angle_rads[:, 1::2] = np.cos(angle_rads[:, 1::2])
        pos_encoding = angle_rads[np.newaxis, ...]
        return tf.cast(pos_encoding, dtype=tf.float32)

    def call(self, inputs):
        return inputs + self.pos_encoding[:, :tf.shape(inputs)[1], :]


class StudyTransformer:
    def __init__(self, max_len=30, embed_dim=64, num_heads=4, ff_dim=128, num_blocks=2):
        """
        Args:
            max_len: 최대 시퀀스 길이 (일 단위)
            embed_dim: 임베딩 차원
            num_heads: 어텐션 헤드 수
            ff_dim: 피드포워드 네트워크 차원
            num_blocks: Transformer 블록 수
        """
        self.max_len = max_len
        self.embed_dim = embed_dim
        self.num_heads = num_heads
        self.ff_dim = ff_dim
        self.num_blocks = num_blocks
        self.model = self._build_model()

    def _build_model(self):
        """Transformer 모델 구성"""
        inputs = keras.Input(shape=(self.max_len, self.embed_dim))
        
        # 위치 인코딩
        x = PositionalEncoding(self.max_len, self.embed_dim)(inputs)
        
        # Transformer 블록들
        for _ in range(self.num_blocks):
            x = TransformerBlock(self.embed_dim, self.num_heads, self.ff_dim)(x)
        
        # Global Average Pooling
        x = layers.GlobalAveragePooling1D()(x)
        
        # 출력 레이어
        x = layers.Dropout(0.1)(x)
        x = layers.Dense(64, activation="relu")(x)
        x = layers.Dropout(0.1)(x)
        
        # 다중 출력: 과목별 예상 학습 시간 (6개 과목)
        outputs = layers.Dense(6, activation="linear", name="study_time_prediction")(x)
        
        model = keras.Model(inputs=inputs, outputs=outputs)
        return model

    def compile_model(self, learning_rate=0.001):
        """모델 컴파일"""
        self.model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=learning_rate),
            loss="mse",
            metrics=["mae"]
        )

    def train(self, train_sequences, train_labels, epochs=50, batch_size=16, validation_split=0.2):
        """
        모델 학습
        
        Args:
            train_sequences: 학습 시퀀스 데이터 (samples, max_len, embed_dim)
            train_labels: 레이블 (samples, 6) - 과목별 예상 학습 시간
            epochs: 학습 에폭 수
            batch_size: 배치 크기
            validation_split: 검증 데이터 비율
        """
        callbacks = [
            keras.callbacks.EarlyStopping(
                monitor='val_loss',
                patience=5,
                restore_best_weights=True
            ),
            keras.callbacks.ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=3
            )
        ]
        
        history = self.model.fit(
            train_sequences,
            train_labels,
            epochs=epochs,
            batch_size=batch_size,
            validation_split=validation_split,
            callbacks=callbacks,
            verbose=1
        )
        return history

    def predict_study_time(self, sequence):
        """
        학습 시간 예측
        
        Args:
            sequence: 최근 학습 패턴 시퀀스 (max_len, embed_dim)
        
        Returns:
            과목별 예상 학습 시간 (6개 과목)
        """
        if len(sequence.shape) == 2:
            sequence = np.expand_dims(sequence, axis=0)
        
        predictions = self.model.predict(sequence)
        return predictions[0]

    def save_model(self, filepath):
        """모델 저장"""
        self.model.save(filepath)

    def load_model(self, filepath):
        """모델 로드"""
        self.model = keras.models.load_model(filepath, custom_objects={
            'TransformerBlock': TransformerBlock,
            'PositionalEncoding': PositionalEncoding
        })


def prepare_sequence_data(study_data, window_size=30):
    """
    시계열 학습 데이터를 시퀀스 형태로 변환
    
    Args:
        study_data: 학습 기록 데이터프레임 (날짜순 정렬)
        window_size: 윈도우 크기 (일 단위)
    
    Returns:
        sequences: 시퀀스 데이터 (samples, window_size, features)
        labels: 다음 날 과목별 학습 시간 (samples, 6)
    """
    sequences = []
    labels = []
    
    # 날짜별로 그룹화
    grouped = study_data.groupby(study_data['start_time'].dt.date)
    dates = sorted(grouped.groups.keys())
    
    subject_map = {'국어': 0, '수학': 1, '영어': 2, '역사': 3, '통합과학': 4, '사회': 5}
    
    for i in range(len(dates) - window_size):
        window_dates = dates[i:i+window_size]
        target_date = dates[i+window_size]
        
        # 윈도우 내 데이터 추출
        window_features = []
        for date in window_dates:
            day_data = grouped.get_group(date)
            
            # 일일 특징 추출
            total_time = day_data['actual_time'].sum()
            subject_times = [0] * 6
            
            for _, row in day_data.iterrows():
                if row['subject'] in subject_map:
                    subject_times[subject_map[row['subject']]] += row['actual_time']
            
            # 정규화
            if total_time > 0:
                subject_times = [t / total_time for t in subject_times]
            
            # 추가 특징
            avg_difficulty = {'쉬움': 0.33, '보통': 0.66, '어려움': 1.0}
            difficulty_score = day_data['difficulty'].map(avg_difficulty).mean()
            
            day_feature = [total_time / 480] + subject_times + [difficulty_score]  # 8시간 기준 정규화
            
            # 64차원으로 패딩
            day_feature += [0] * (64 - len(day_feature))
            window_features.append(day_feature)
        
        # 다음 날 레이블 (과목별 학습 시간)
        target_data = grouped.get_group(target_date)
        target_times = [0] * 6
        
        for _, row in target_data.iterrows():
            if row['subject'] in subject_map:
                target_times[subject_map[row['subject']]] += row['actual_time']
        
        sequences.append(window_features)
        labels.append(target_times)
    
    return np.array(sequences, dtype=np.float32), np.array(labels, dtype=np.float32)


def generate_daily_plan_with_transformer(transformer_model, recent_history, target_grade_adjustments):
    """
    Transformer를 사용하여 일일 학습 계획 생성
    
    Args:
        transformer_model: 학습된 Transformer 모델
        recent_history: 최근 학습 기록 시퀀스
        target_grade_adjustments: 목표 등급별 과목 조정 계수 dict
    
    Returns:
        과목별 추천 학습 시간 (분 단위)
    """
    predicted_times = transformer_model.predict_study_time(recent_history)
    
    # 목표 등급에 따라 조정
    adjusted_times = []
    subjects = ['국어', '수학', '영어', '역사', '통합과학', '사회']
    
    for i, subject in enumerate(subjects):
        base_time = predicted_times[i]
        adjustment = target_grade_adjustments.get(subject, 1.0)
        adjusted_time = base_time * adjustment
        adjusted_times.append(max(0, adjusted_time))  # 음수 방지
    
    return dict(zip(subjects, adjusted_times))


if __name__ == '__main__':
    # 예제 사용법
    print("Transformer 모델 초기화...")
    transformer = StudyTransformer(
        max_len=30,
        embed_dim=64,
        num_heads=4,
        ff_dim=128,
        num_blocks=2
    )
    transformer.compile_model(learning_rate=0.001)
    
    # 더미 데이터로 테스트
    dummy_sequences = np.random.rand(500, 30, 64).astype(np.float32)
    dummy_labels = np.random.rand(500, 6).astype(np.float32) * 120  # 0-120분
    
    print("모델 학습 시작...")
    history = transformer.train(dummy_sequences, dummy_labels, epochs=10, batch_size=16)
    
    print("모델 저장...")
    transformer.save_model('transformer_study_planner.h5')
    
    print("학습 시간 예측 테스트...")
    test_sequence = np.random.rand(30, 64).astype(np.float32)
    predicted_time = transformer.predict_study_time(test_sequence)
    print(f"예측된 과목별 학습 시간: {predicted_time}")
    
    print("Transformer 모델 준비 완료!")
