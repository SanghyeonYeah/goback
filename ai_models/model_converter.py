"""
AI 모델을 TFLite 형식으로 변환하여 Android 앱에서 사용 가능하도록 경량화
"""

import tensorflow as tf
import numpy as np
import os
from pathlib import Path

class ModelConverter:
    """TensorFlow 모델을 TFLite로 변환"""
    
    def __init__(self, output_dir='tflite_models'):
        self.output_dir = output_dir
        Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    def convert_to_tflite(self, model_path, model_name, quantize=True, representative_dataset=None):
        """
        모델을 TFLite 형식으로 변환
        
        Args:
            model_path: 변환할 모델 경로 (.h5)
            model_name: 출력 파일명
            quantize: 양자화 적용 여부
            representative_dataset: 양자화를 위한 대표 데이터셋
        
        Returns:
            변환된 TFLite 모델 경로
        """
        print(f"[{model_name}] 모델 로딩 중...")
        model = tf.keras.models.load_model(model_path)
        
        # TFLite 변환기 설정
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        
        if quantize:
            print(f"[{model_name}] 양자화 적용 중...")
            # Dynamic range quantization (기본)
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            
            # Full integer quantization (representative dataset 필요)
            if representative_dataset is not None:
                converter.representative_dataset = representative_dataset
                converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
                converter.inference_input_type = tf.uint8
                converter.inference_output_type = tf.uint8
        
        # 변환 실행
        print(f"[{model_name}] 변환 중...")
        tflite_model = converter.convert()
        
        # 저장
        output_path = os.path.join(self.output_dir, f'{model_name}.tflite')
        with open(output_path, 'wb') as f:
            f.write(tflite_model)
        
        # 모델 크기 확인
        original_size = os.path.getsize(model_path) / (1024 * 1024)
        tflite_size = os.path.getsize(output_path) / (1024 * 1024)
        compression_ratio = (1 - tflite_size / original_size) * 100
        
        print(f"[{model_name}] 변환 완료!")
        print(f"  원본 크기: {original_size:.2f} MB")
        print(f"  TFLite 크기: {tflite_size:.2f} MB")
        print(f"  압축률: {compression_ratio:.2f}%")
        print(f"  저장 경로: {output_path}")
        
        return output_path
    
    def test_tflite_model(self, tflite_path, test_input):
        """
        변환된 TFLite 모델 테스트
        
        Args:
            tflite_path: TFLite 모델 경로
            test_input: 테스트 입력 데이터
        
        Returns:
            모델 출력
        """
        print(f"TFLite 모델 테스트: {tflite_path}")
        
        # Interpreter 생성
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
        
        # 입력/출력 정보
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print("입력 정보:", input_details)
        print("출력 정보:", output_details)
        
        # 테스트 실행
        interpreter.set_tensor(input_details[0]['index'], test_input)
        interpreter.invoke()
        output = interpreter.get_tensor(output_details[0]['index'])
        
        print("테스트 성공!")
        print("출력 shape:", output.shape)
        
        return output


def create_representative_dataset(sample_data, batch_size=100):
    """
    Full integer quantization을 위한 대표 데이터셋 생성
    
    Args:
        sample_data: 샘플 데이터
        batch_size: 배치 크기
    
    Returns:
        대표 데이터셋 제너레이터
    """
    def representative_dataset_gen():
        for i in range(min(batch_size, len(sample_data))):
            yield [sample_data[i:i+1].astype(np.float32)]
    
    return representative_dataset_gen


def convert_all_models():
    """모든 AI 모델을 TFLite로 변환"""
    converter = ModelConverter(output_dir='../app/src/main/assets/models')
    
    # 1. VAE 모델 변환
    print("\n" + "="*50)
    print("VAE 모델 변환")
    print("="*50)
    
    vae_test_input = np.random.rand(1, 128).astype(np.float32)
    vae_repr_dataset = create_representative_dataset(
        np.random.rand(100, 128).astype(np.float32)
    )
    
    try:
        vae_path = converter.convert_to_tflite(
            'vae_study_planner.h5',
            'vae_model',
            quantize=True,
            representative_dataset=vae_repr_dataset
        )
        converter.test_tflite_model(vae_path, vae_test_input)
    except Exception as e:
        print(f"VAE 변환 실패: {e}")
    
    # 2. Transformer 모델 변환
    print("\n" + "="*50)
    print("Transformer 모델 변환")
    print("="*50)
    
    transformer_test_input = np.random.rand(1, 30, 64).astype(np.float32)
    transformer_repr_dataset = create_representative_dataset(
        np.random.rand(100, 30, 64).astype(np.float32)
    )
    
    try:
        transformer_path = converter.convert_to_tflite(
            'transformer_study_planner.h5',
            'transformer_model',
            quantize=True,
            representative_dataset=transformer_repr_dataset
        )
        converter.test_tflite_model(transformer_path, transformer_test_input)
    except Exception as e:
        print(f"Transformer 변환 실패: {e}")
    
    # 3. GNN 모델 변환 (별도 파일에서 생성 필요)
    print("\n" + "="*50)
    print("GNN 모델 변환")
    print("="*50)
    
    if os.path.exists('gnn_study_planner.h5'):
        gnn_test_input = np.random.rand(1, 6, 32).astype(np.float32)  # 6개 과목
        gnn_repr_dataset = create_representative_dataset(
            np.random.rand(100, 6, 32).astype(np.float32)
        )
        
        try:
            gnn_path = converter.convert_to_tflite(
                'gnn_study_planner.h5',
                'gnn_model',
                quantize=True,
                representative_dataset=gnn_repr_dataset
            )
            converter.test_tflite_model(gnn_path, gnn_test_input)
        except Exception as e:
            print(f"GNN 변환 실패: {e}")
    else:
        print("GNN 모델을 찾을 수 없습니다. 먼저 gnn_model.py를 실행하세요.")
    
    # 4. DQN 모델 변환 (별도 파일에서 생성 필요)
    print("\n" + "="*50)
    print("DQN 모델 변환")
    print("="*50)
    
    if os.path.exists('dqn_study_planner.h5'):
        dqn_test_input = np.random.rand(1, 50).astype(np.float32)  # 상태 벡터
        dqn_repr_dataset = create_representative_dataset(
            np.random.rand(100, 50).astype(np.float32)
        )
        
        try:
            dqn_path = converter.convert_to_tflite(
                'dqn_study_planner.h5',
                'dqn_model',
                quantize=True,
                representative_dataset=dqn_repr_dataset
            )
            converter.test_tflite_model(dqn_path, dqn_test_input)
        except Exception as e:
            print(f"DQN 변환 실패: {e}")
    else:
        print("DQN 모델을 찾을 수 없습니다. 먼저 dqn_model.py를 실행하세요.")
    
    print("\n" + "="*50)
    print("모든 모델 변환 완료!")
    print("="*50)
    print(f"변환된 모델 저장 위치: {converter.output_dir}")


def export_model_metadata():
    """모델 메타데이터 JSON 파일 생성"""
    import json
    
    metadata = {
        "vae_model": {
            "input_shape": [1, 128],
            "output_shape": [1, 128],
            "description": "학습 패턴 인코딩 및 생성 모델",
            "usage": "1등급 학생 패턴 기반 학습 계획 생성"
        },
        "transformer_model": {
            "input_shape": [1, 30, 64],
            "output_shape": [1, 6],
            "description": "시계열 학습 데이터 분석 모델",
            "usage": "최근 30일 학습 기록 기반 과목별 학습 시간 예측"
        },
        "gnn_model": {
            "input_shape": [1, 6, 32],
            "output_shape": [1, 6],
            "description": "과목 간 관계 모델링",
            "usage": "과목 간 연관성 분석 및 균형잡힌 학습 계획"
        },
        "dqn_model": {
            "input_shape": [1, 50],
            "output_shape": [1, 10],
            "description": "학습 계획 최적화 모델",
            "usage": "강화학습 기반 최적 학습 전략 선택"
        }
    }
    
    with open('../app/src/main/assets/models/model_metadata.json', 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    
    print("모델 메타데이터 저장 완료!")


if __name__ == '__main__':
    print("Study Planner AI 모델 변환기")
    print("="*50)
    
    # 모든 모델 변환
    convert_all_models()
    
    # 메타데이터 생성
    export_model_metadata()
    
    print("\n변환 프로세스 완료!")
    print("Android 앱의 assets/models 폴더에 TFLite 모델이 저장되었습니다.")
