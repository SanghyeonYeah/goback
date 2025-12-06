"""
DQN (Deep Q-Network) 모델
강화학습을 통한 학습 계획 최적화
"""

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np
from collections import deque
import random

class DQNAgent:
    """Deep Q-Network 에이전트"""
    
    def __init__(self, state_size=50, action_size=10, learning_rate=0.001):
        """
        Args:
            state_size: 상태 벡터 크기
            action_size: 행동 개수
            learning_rate: 학습률
        """
        self.state_size = state_size
        self.action_size = action_size
        self.learning_rate = learning_rate
        
        # Hyperparameters
        self.gamma = 0.95  # Discount factor
        self.epsilon = 1.0  # Exploration rate
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.batch_size = 32
        
        # Replay memory
        self.memory = deque(maxlen=2000)
        
        # Models
        self.model = self._build_model()
        self.target_model = self._build_model()
        self.update_target_model()
        
    def _build_model(self):
        """Q-Network 구성"""
        model = keras.Sequential([
            layers.Input(shape=(self.state_size,)),
            layers.Dense(128, activation='relu'),
            layers.BatchNormalization(),
            layers.Dropout(0.2),
            layers.Dense(128, activation='relu'),
            layers.BatchNormalization(),
            layers.Dropout(0.2),
            layers.Dense(64, activation='relu'),
            layers.Dense(self.action_size, activation='linear')
        ])
        
        model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=self.learning_rate),
            loss='mse'
        )
        return model
    
    def update_target_model(self):
        """타겟 네트워크 가중치 업데이트"""
        self.target_model.set_weights(self.model.get_weights())
    
    def remember(self, state, action, reward, next_state, done):
        """경험을 메모리에 저장"""
        self.memory.append((state, action, reward, next_state, done))
    
    def act(self, state, training=True):
        """
        현재 상태에서 행동 선택
        
        Args:
            state: 현재 상태
            training: 학습 모드 여부 (탐험 vs 활용)
        
        Returns:
            선택된 행동
        """
        if training and np.random.rand() <= self.epsilon:
            # Exploration: 랜덤 행동
            return random.randrange(self.action_size)
        
        # Exploitation: Q-value가 최대인 행동
        state = np.reshape(state, [1, self.state_size])
        q_values = self.model.predict(state, verbose=0)
        return np.argmax(q_values[0])
    
    def replay(self):
        """경험 재생을 통한 학습"""
        if len(self.memory) < self.batch_size:
            return
        
        # 랜덤 샘플링
        minibatch = random.sample(self.memory, self.batch_size)
        
        states = np.array([experience[0] for experience in minibatch])
        actions = np.array([experience[1] for experience in minibatch])
        rewards = np.array([experience[2] for experience in minibatch])
        next_states = np.array([experience[3] for experience in minibatch])
        dones = np.array([experience[4] for experience in minibatch])
        
        # 현재 Q-values
        current_q = self.model.predict(states, verbose=0)
        
        # 타겟 Q-values
        next_q = self.target_model.predict(next_states, verbose=0)
        
        # Q-learning 업데이트
        for i in range(self.batch_size):
            if dones[i]:
                current_q[i][actions[i]] = rewards[i]
            else:
                current_q[i][actions[i]] = rewards[i] + self.gamma * np.max(next_q[i])
        
        # 모델 학습
        self.model.fit(states, current_q, epochs=1, verbose=0)
        
        # Epsilon 감소 (탐험 비율 줄이기)
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay
    
    def save_model(self, filepath):
        """모델 저장"""
        self.model.save(filepath)
    
    def load_model(self, filepath):
        """모델 로드"""
        self.model = keras.models.load_model(filepath)
        self.update_target_model()


class StudyPlanEnvironment:
    """학습 계획 환경 (강화학습용)"""
    
    def __init__(self):
        # 상태 공간 정의
        self.state_size = 50
        
        # 행동 공간 정의
        # 0-5: 각 과목에 시간 추가
        # 6-9: 학습 전략 변경 (난이도 조정, 활동 유형 변경 등)
        self.action_size = 10
        
        # 초기화
        self.reset()
    
    def reset(self):
        """환경 초기화"""
        self.current_state = self._generate_initial_state()
        self.steps = 0
        self.max_steps = 30  # 30일
        return self.current_state
    
    def _generate_initial_state(self):
        """초기 상태 생성"""
        # 상태: [과목별 누적 시간(6), 과목별 정확도(6), 과목별 목표 등급(6), 
        #       현재 등급(6), 남은 기간, 전체 진행률, 기타 특징(24)]
        state = np.random.rand(self.state_size).astype(np.float32)
        return state
    
    def step(self, action):
        """
        행동 실행
        
        Args:
            action: 선택된 행동
        
        Returns:
            next_state: 다음 상태
            reward: 보상
            done: 종료 여부
        """
        # 행동에 따른 상태 변화
        next_state = self._apply_action(self.current_state, action)
        
        # 보상 계산
        reward = self._calculate_reward(self.current_state, next_state, action)
        
        # 종료 조건
        self.steps += 1
        done = self.steps >= self.max_steps
        
        self.current_state = next_state
        return next_state, reward, done
    
    def _apply_action(self, state, action):
        """행동을 상태에 적용"""
        next_state = state.copy()
        
        if action < 6:
            # 과목별 시간 추가
            next_state[action] += 0.1
        elif action == 6:
            # 난이도 조정
            next_state[40] = min(next_state[40] + 0.05, 1.0)
        elif action == 7:
            # 복습 비율 증가
            next_state[41] = min(next_state[41] + 0.1, 1.0)
        elif action == 8:
            # 문제풀이 집중
            next_state[42] = min(next_state[42] + 0.1, 1.0)
        elif action == 9:
            # 개념학습 집중
            next_state[43] = min(next_state[43] + 0.1, 1.0)
        
        # 진행률 업데이트
        next_state[-2] = self.steps / self.max_steps
        
        return next_state
    
    def _calculate_reward(self, state, next_state, action):
        """보상 계산"""
        # 기본 보상
        reward = 0
        
        # 목표 등급과 현재 등급 차이
        target_grades = state[12:18]
        current_grades = state[18:24]
        grade_diff = np.sum(np.abs(target_grades - current_grades))
        
        next_current_grades = next_state[18:24]
        next_grade_diff = np.sum(np.abs(target_grades - next_current_grades))
        
        # 등급 차이가 줄어들면 보상
        if next_grade_diff < grade_diff:
            reward += 10
        
        # 균형잡힌 학습 보상
        study_times = next_state[0:6]
        if np.std(study_times) < 0.3:  # 표준편차가 작으면 균형잡힘
            reward += 5
        
        # 과도한 학습 시간 페널티
        total_time = np.sum(study_times)
        if total_time > 5.0:  # 너무 많은 시간
            reward -= 5
        
        # 목표 달성 보상
        if next_grade_diff < 0.5:
            reward += 20
        
        return reward


def train_dqn_agent(episodes=100, save_path='dqn_study_planner.h5'):
    """DQN 에이전트 학습"""
    env = StudyPlanEnvironment()
    agent = DQNAgent(
        state_size=env.state_size,
        action_size=env.action_size,
        learning_rate=0.001
    )
    
    scores = []
    
    for episode in range(episodes):
        state = env.reset()
        total_reward = 0
        
        for step in range(env.max_steps):
            # 행동 선택
            action = agent.act(state, training=True)
            
            # 환경에서 실행
            next_state, reward, done = env.step(action)
            
            # 경험 저장
            agent.remember(state, action, reward, next_state, done)
            
            state = next_state
            total_reward += reward
            
            # 학습
            agent.replay()
            
            if done:
                break
        
        # 타겟 모델 업데이트
        if episode % 10 == 0:
            agent.update_target_model()
        
        scores.append(total_reward)
        
        if episode % 10 == 0:
            avg_score = np.mean(scores[-10:])
            print(f"Episode: {episode}/{episodes}, "
                  f"Score: {total_reward:.2f}, "
                  f"Avg Score: {avg_score:.2f}, "
                  f"Epsilon: {agent.epsilon:.3f}")
    
    # 모델 저장
    agent.save_model(save_path)
    print(f"\n모델 저장 완료: {save_path}")
    
    return agent, scores


def optimize_study_plan_with_dqn(dqn_agent, current_state):
    """
    DQN을 사용하여 최적 학습 전략 선택
    
    Args:
        dqn_agent: 학습된 DQN 에이전트
        current_state: 현재 학습 상태
    
    Returns:
        최적 행동 (학습 전략)
    """
    action = dqn_agent.act(current_state, training=False)
    
    action_descriptions = {
        0: "국어 학습 시간 증가",
        1: "수학 학습 시간 증가",
        2: "영어 학습 시간 증가",
        3: "역사 학습 시간 증가",
        4: "통합과학 학습 시간 증가",
        5: "사회 학습 시간 증가",
        6: "난이도 상향 조정",
        7: "복습 비율 증가",
        8: "문제풀이 집중",
        9: "개념학습 집중"
    }
    
    return action, action_descriptions[action]


if __name__ == '__main__':
    print("DQN 에이전트 학습 시작...")
    agent, scores = train_dqn_agent(episodes=100)
    
    print("\n학습 완료!")
    print(f"평균 점수: {np.mean(scores):.2f}")
    print(f"최고 점수: {np.max(scores):.2f}")
    
    # 테스트
    print("\n최적 전략 테스트...")
    test_state = np.random.rand(50).astype(np.float32)
    action, description = optimize_study_plan_with_dqn(agent, test_state)
    print(f"추천 전략: {description}")
    
    print("\nDQN 모델 준비 완료!")
