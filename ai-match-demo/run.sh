#!/usr/bin/env bash
# Cài đặt (lần đầu) + chạy demo trên Linux Mint / Ubuntu
set -e
cd "$(dirname "$0")"

if [ ! -d .venv ]; then
  echo ">>> Cài Python venv..."
  sudo apt update
  sudo apt install -y python3 python3-venv python3-pip
  python3 -m venv .venv
  source .venv/bin/activate
  pip install --upgrade pip
  echo ">>> Cài PyTorch bản CPU (nhẹ hơn nhiều so với bản GPU)..."
  pip install torch --index-url https://download.pytorch.org/whl/cpu
  echo ">>> Cài các thư viện còn lại..."
  pip install -r requirements.txt
else
  source .venv/bin/activate
fi

echo ">>> Mở trình duyệt tại http://localhost:8501"
streamlit run app.py
