import sys
import json
import numpy as np
import tensorflow as tf
from datetime import datetime, timedelta

# TFLite 모델 로드
interpreter = tf.lite.Interpreter(model_path="ai/todo_transformer_opt.tflite")
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

def predict_todos(input_json):
    data = json.loads(input_json)
    goals = data['goals']
    study_period = data['studyPeriod']

    # 입력 데이터 구성 (예: 등급 → 숫자)
    subjects = ["korean","math","english","social","science","history"]
    x_input = np.array([[goals.get(sub,3) for sub in subjects]], dtype=np.float32)

    interpreter.set_tensor(input_details[0]['index'], x_input)
    interpreter.invoke()
    out = interpreter.get_tensor(output_details[0]['index'])

    # Todo 리스트 변환
    tasks = ['개념 학습', '문제 풀이', '복습', '심화 학습']
    difficulties = ['쉬움','보통','어려움']

    todos = []
    today = datetime.today()
    for day in range(study_period):
        for i, sub in enumerate(subjects):
            task_count = 3 if goals.get(sub,3)<=2 else 2 if goals.get(sub,3)<=3 else 1
            for _ in range(task_count):
                todo = {
                    "date": (today + timedelta(days=day)).strftime("%Y-%m-%d"),
                    "subject": sub.capitalize(),
                    "task": np.random.choice(tasks),
                    "difficulty": difficulties[min(int(goals.get(sub,3))-1,2)]
                }
                todos.append(todo)
    return todos

if __name__=="__main__":
    input_json = sys.stdin.read()
    todos = predict_todos(input_json)
    print(json.dumps(todos, ensure_ascii=False))
