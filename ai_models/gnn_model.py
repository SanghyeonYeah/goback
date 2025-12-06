"""
GNN (Graph Neural Network) 모델
과목 간 관계를 모델링하여 균형잡힌 학습 계획 생성
"""

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np

class GNNLayer(layers.Layer):
    """Graph Convolutional Layer"""
    def __init__(self, units, activation='relu'):
        super(GNNLayer, self).__init__()
        self.units = units
        self.activation = keras.activations.get(activation)
        
    def build(self, input_shape):
        # Node feature transformation
        self.W = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer='glorot_uniform',
            trainable=True,
            name='W'
        )
        self.b = self.add_weight(
            shape=(self.units,),
            initializer='zeros',
            trainable=True,
            name='b'
        )
        
    def call(self, inputs, adjacency_matrix):
        """
        Args:
            inputs: Node features (batch, num_nodes, features)
            adjacency_matrix: Adjacency matrix (num_nodes, num_nodes)
        """
        # Message passing: aggregate neighbor information
        # A * X
        aggregated = tf.matmul(adjacency_matrix, inputs)
        
        # Transform: A * X * W + b
        output = tf.matmul(aggregated, self.W) + self.b
        
        if self.activation is not None:
            output = self.activation(output)
            
        return output


class SubjectGNN:
    """과목 간 관계를 학습하는 GNN 모델"""
    
    def __init__(self, num_subjects=6, node_features=32, hidden_units=64):
        """
        Args:
            num_subjects: 과목 수 (기본 6개)
            node_features: 각 노드(과목)의 특징 차원
            hidden_units: 히든 레이어 유닛 수
        """
        self.num_subjects = num_subjects
        self.node_features = node_features
        self.hidden_units = hidden_units
        self.model = self._build_model()
        self.adjacency_matrix = self._create_subject_adjacency()
        
    def _create_subject_adjacency(self):
        """
        과목 간 인접 행렬 생성
        과목 간 관계를 정의 (예: 수학-과학은 강한 연관성)
        """
        # 과목 순서: 국어, 수학, 영어, 역사, 통합과학, 사회
        adj = np.array([
            # 국어  수학  영어  역사  과학  사회
            [1.0, 0.3, 0.5, 0.7, 0.2, 0.6],  # 국어
            [0.3, 1.0, 0.4, 0.2, 0.9, 0.5],  # 수학
            [0.5, 0.4, 1.0, 0.3, 0.3, 0.4],  # 영어
            [0.7, 0.2, 0.3, 1.0, 0.3, 0.8],  # 역사
            [0.2, 0.9, 0.3, 0.3, 1.0, 0.4],  # 통합과학
            [0.6, 0.5, 0.4, 0.8, 0.4, 1.0],  # 사회
        ], dtype=np.float32)
        
        # Normalize adjacency matrix (D^-1/2 * A * D^-1/2)
        degree = np.sum(adj, axis=1)
        degree_inv_sqrt = np.power(degree, -0.5)
        degree_inv_sqrt[np.isinf(degree_inv_sqrt)] = 0.
        degree_matrix = np.diag(degree_inv_sqrt)
        
        normalized_adj = degree_matrix @ adj @ degree_matrix
        return tf.constant(normalized_adj, dtype=tf.float32)
    
    def _build_model(self):
        """GNN 모델 구성"""
        # 입력: 각 과목의 특징 벡터
        node_features = keras.Input(
            shape=(self.num_subjects, self.node_features),
            name='node_features'
        )
        
        # First GNN layer
        x = GNNLayer(self.hidden_units, activation='relu')(
            node_features, 
            self.adjacency_matrix
        )
        x = layers.Dropout(0.3)(x)
        
        # Second GNN layer
        x = GNNLayer(self.hidden_units, activation='relu')(
            x,
            self.adjacency_matrix
        )
        x = layers.Dropout(0.3)(x)
        
        # Third GNN layer
        x = GNNLayer(32, activation='relu')(
            x,
            self.adjacency_matrix
        )
        
        # Global pooling to get graph-level representation
        graph_repr = layers.GlobalAveragePooling1D()(x)
        
        # Output: 과목별 학습 시간 가중치
        outputs = layers.Dense(
            self.num_subjects,
            activation='softmax',
            name='subject_weights'
        )(graph_repr)
        
        model = keras.Model(inputs=node_features, outputs=outputs)
        return model
    
    def compile_model(self, learning_rate=0.001):
        """모델 컴파일"""
        self.model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=learning_rate),
            loss='categorical_crossentropy',
            metrics=['accuracy', 'mae']
        )
    
    def train(self, train_features, train_labels, epochs=50, batch_size=16, validation_split=0.2):
        """
        모델 학습
        
        Args:
            train_features: 과목별 특징 (samples, num_subjects, node_features)
            train_labels: 과목별 최적 시간 배분 (samples, num_subjects)
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
            train_features,
            train_labels,
            epochs=epochs,
            batch_size=batch_size,
            validation_split=validation_split,
            callbacks=callbacks,
            verbose=1
        )
        return history
    
    def predict_subject_weights(self, subject_features):
        """
        과목별 학습 시간 가중치 예측
        
        Args:
            subject_features: 과목별 특징 (num_subjects, node_features)
        
        Returns:
            과목별 가중치 (num_subjects,)
        """
        if len(subject_features.shape) == 2:
            subject_features = np.expand_dims(subject_features, axis=0)
        
        weights = self.model.predict(subject_features)
        return weights[0]
    
    def save_model(self, filepath):
        """모델 저장"""
        self.model.save(filepath)
    
    def load_model(self, filepath):
        """모델 로드"""
        self.model = keras.models.load_model(filepath, custom_objects={
            'GNNLayer': GNNLayer
        })


def create_subject_features(user_data, goals):
    """
    사용자 데이터를 기반으로 과목별 특징 벡터 생성
    
    Args:
        user_data: 사용자의 학습 기록
        goals: 과목별 목표 등급
    
    Returns:
        subject_features: (num_subjects, node_features)
    """
    subjects = ['국어', '수학', '영어', '역사', '통합과학', '사회']
    features = []
    
    for subject in subjects:
        subject_data = user_data[user_data['subject'] == subject]
        
        # 특징 추출
        total_time = subject_data['actual_time'].sum()
        avg_accuracy = subject_data.get('accuracy', 0.5).mean() if len(subject_data) > 0 else 0.5
        num_problems = len(subject_data)
        
        # 목표 등급 (1-5 → 0-1 정규화)
        target_grade = goals.get(subject, 3)
        normalized_grade = (6 - target_grade) / 4  # 1등급 = 1.0, 5등급 = 0.25
        
        # 최근 학습 빈도
        recent_frequency = len(subject_data[subject_data['start_time'] > 
                                          pd.Timestamp.now() - pd.Timedelta(days=7)])
        
        # 난이도별 분포
        difficulty_dist = subject_data['difficulty'].value_counts(normalize=True)
        easy_ratio = difficulty_dist.get('쉬움', 0)
        medium_ratio = difficulty_dist.get('보통', 0)
        hard_ratio = difficulty_dist.get('어려움', 0)
        
        # 특징 벡터 구성 (32차원)
        feature = [
            total_time / 1000,  # 정규화
            avg_accuracy,
            num_problems / 100,
            normalized_grade,
            recent_frequency / 10,
            easy_ratio,
            medium_ratio,
            hard_ratio
        ]
        
        # 32차원으로 패딩
        feature += [0] * (32 - len(feature))
        features.append(feature)
    
    return np.array(features, dtype=np.float32)


def balance_study_plan_with_gnn(gnn_model, user_data, goals, total_study_time=300):
    """
    GNN을 사용하여 균형잡힌 학습 계획 생성
    
    Args:
        gnn_model: 학습된 GNN 모델
        user_data: 사용자 학습 데이터
        goals: 과목별 목표 등급
        total_study_time: 총 학습 시간 (분)
    
    Returns:
        과목별 학습 시간 배분 (dict)
    """
    # 과목별 특징 생성
    subject_features = create_subject_features(user_data, goals)
    
    # GNN으로 가중치 예측
    weights = gnn_model.predict_subject_weights(subject_features)
    
    # 목표 등급에 따라 조정
    adjusted_weights = []
    subjects = ['국어', '수학', '영어', '역사', '통합과학', '사회']
    
    for i, subject in enumerate(subjects):
        target_grade = goals.get(subject, 3)
        # 목표 등급이 높을수록 더 많은 시간 배정
        grade_factor = (6 - target_grade) / 2  # 1등급: 2.5, 5등급: 0.5
        adjusted_weights.append(weights[i] * grade_factor)
    
    # 정규화
    adjusted_weights = np.array(adjusted_weights)
    adjusted_weights = adjusted_weights / adjusted_weights.sum()
    
    # 시간 배분
    time_allocation = {}
    for i, subject in enumerate(subjects):
        time_allocation[subject] = int(total_study_time * adjusted_weights[i])
    
    return time_allocation


if __name__ == '__main__':
    # 예제 사용법
    print("GNN 모델 초기화...")
    gnn = SubjectGNN(
        num_subjects=6,
        node_features=32,
        hidden_units=64
    )
    gnn.compile_model(learning_rate=0.001)
    
    # 더미 데이터로 테스트
    dummy_features = np.random.rand(500, 6, 32).astype(np.float32)
    dummy_labels = np.random.rand(500, 6).astype(np.float32)
    dummy_labels = dummy_labels / dummy_labels.sum(axis=1, keepdims=True)  # Normalize
    
    print("모델 학습 시작...")
    history = gnn.train(dummy_features, dummy_labels, epochs=10, batch_size=16)
    
    print("모델 저장...")
    gnn.save_model('gnn_study_planner.h5')
    
    print("과목별 가중치 예측 테스트...")
    test_features = np.random.rand(6, 32).astype(np.float32)
    weights = gnn.predict_subject_weights(test_features)
    
    subjects = ['국어', '수학', '영어', '역사', '통합과학', '사회']
    print("\n과목별 학습 시간 가중치:")
    for subject, weight in zip(subjects, weights):
        print(f"  {subject}: {weight:.3f} ({weight*100:.1f}%)")
    
    print("\nGNN 모델 준비 완료!")
