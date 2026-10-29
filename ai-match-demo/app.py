import hashlib
import json
import os
import re
from pathlib import Path

import numpy as np
import streamlit as st
from google import genai
from google.genai import types

EMBED_MODEL = "gemini-embedding-001"
DIM = 768
BASE = Path(__file__).parent
CACHE_FILE = BASE / "embeddings_cache.npz"

PROFILES = json.loads((BASE / "profiles.json").read_text(encoding="utf-8"))

EXAMPLES = [
    "Tôi muốn tìm một bạn nữ thích đọc sách và đi dạo phố",
    "Tìm người sống chậm, điềm tĩnh, thích sự yên tĩnh",
    "Tìm người yêu động vật, thích nuôi thú cưng",
    "Tìm người năng động, thích khám phá và vận động ngoài trời",
]

STOPWORDS = {
    "tôi", "muốn", "tìm", "một", "bạn", "người", "thích", "và", "là", "có",
    "của", "hay", "đi", "cho", "với", "những", "rất", "nữ", "nam",
}

st.set_page_config(page_title="AI Match Demo", page_icon="💘", layout="wide")


def profile_text(p):
    return f"{p['gender']}, {p['age']} tuổi, ở {p['city']}. {p['bio']}"


def embed_texts(api_key, texts, task_type):
    client = genai.Client(api_key=api_key)
    vectors = []
    for i in range(0, len(texts), 100):
        res = client.models.embed_content(
            model=EMBED_MODEL,
            contents=texts[i : i + 100],
            config=types.EmbedContentConfig(
                task_type=task_type, output_dimensionality=DIM
            ),
        )
        vectors.extend(e.values for e in res.embeddings)
    arr = np.array(vectors, dtype=np.float32)
    arr /= np.linalg.norm(arr, axis=1, keepdims=True)
    return arr


@st.cache_data(show_spinner="Đang chuyển 24 hồ sơ thành 'tọa độ' (chỉ làm 1 lần)...")
def profile_embeddings(api_key):
    texts = [profile_text(p) for p in PROFILES]
    key = hashlib.sha256(("|".join(texts) + EMBED_MODEL + str(DIM)).encode()).hexdigest()
    if CACHE_FILE.exists():
        data = np.load(CACHE_FILE, allow_pickle=False)
        if str(data["key"]) == key:
            return data["emb"]
    emb = embed_texts(api_key, texts, "RETRIEVAL_DOCUMENT")
    np.savez(CACHE_FILE, key=np.array(key), emb=emb)
    return emb


@st.cache_data(show_spinner="Đang hỏi AI...")
def query_embedding(api_key, query):
    return embed_texts(api_key, [query], "RETRIEVAL_QUERY")[0]


def ai_search(api_key, query, candidate_idx, top_k):
    q = query_embedding(api_key, query)
    emb = profile_embeddings(api_key)
    scores = emb[candidate_idx] @ q
    order = np.argsort(-scores)[:top_k]
    return [(candidate_idx[i], float(scores[i])) for i in order], q


def tokens(text):
    return set(re.findall(r"\w+", text.lower())) - STOPWORDS


def keyword_search(query, candidate_idx, top_k):
    q = tokens(query)
    scored = []
    for i in candidate_idx:
        overlap = len(q & tokens(PROFILES[i]["bio"]))
        if overlap > 0:
            scored.append((i, overlap))
    scored.sort(key=lambda x: -x[1])
    return scored[:top_k]


if "liked" not in st.session_state:
    st.session_state.liked = set()
st.session_state.setdefault("query", EXAMPLES[0])


def set_query(q):
    st.session_state.query = q


def card(p, score_text, key):
    with st.container(border=True):
        st.markdown(f"**{p['name']}**, {p['age']} · {p['gender']} · {p['city']}")
        st.caption(score_text)
        st.write(p["bio"])
        if p["id"] in st.session_state.liked:
            st.success("Đã thích")
        c1, c2 = st.columns(2)
        if c1.button("❤️ Thích", key=f"like-{key}-{p['id']}"):
            st.session_state.liked.add(p["id"])
            st.rerun()
        if c2.button("✖ Bỏ qua", key=f"pass-{key}-{p['id']}"):
            st.session_state.liked.discard(p["id"])
            st.rerun()


with st.sidebar:
    st.header("Gemini API key")
    api_key = st.text_input(
        "Dán key vào đây",
        type="password",
        value=os.environ.get("GEMINI_API_KEY", ""),
        help="Lấy miễn phí tại aistudio.google.com/apikey",
    ).strip()
    st.divider()
    st.header("Bộ lọc cứng")
    st.caption("Dữ liệu có cấu trúc (giới tính, tuổi, thành phố) lọc bằng điều kiện thường, không cần AI.")
    gender = st.radio("Giới tính", ["Tất cả", "Nữ", "Nam"])
    age_min, age_max = st.slider("Độ tuổi", 18, 40, (20, 35))
    cities = sorted({p["city"] for p in PROFILES})
    city_sel = st.multiselect("Thành phố", cities, default=cities)
    top_k = st.slider("Số kết quả", 1, 10, 5)
    compare = st.checkbox("So sánh với tìm theo từ khóa", value=True)
    st.divider()
    st.subheader("Đã thích")
    liked_names = [p["name"] for p in PROFILES if p["id"] in st.session_state.liked]
    st.write(", ".join(liked_names) if liked_names else "Chưa có ai")

st.title("💘 AI Match Demo")

if not api_key:
    st.info(
        "Cần một Gemini API key miễn phí để chạy:\n\n"
        "1. Vào https://aistudio.google.com/apikey và đăng nhập tài khoản Google\n"
        "2. Bấm **Create API key**, rồi copy key\n"
        "3. Dán vào ô bên trái"
    )
    st.stop()

st.write("Gõ mô tả người bạn muốn tìm bằng ngôn ngữ tự nhiên. Ctrl+Enter để tìm.")
st.text_area("Bạn đang tìm ai?", key="query", height=80)
ex_cols = st.columns(len(EXAMPLES))
for col, ex in zip(ex_cols, EXAMPLES):
    col.button(ex, on_click=set_query, args=(ex,), use_container_width=True)

query = st.session_state.query.strip()
candidate_idx = [
    i for i, p in enumerate(PROFILES)
    if (gender == "Tất cả" or p["gender"] == gender)
    and age_min <= p["age"] <= age_max
    and p["city"] in city_sel
]

if not query:
    st.info("Hãy nhập mô tả để bắt đầu.")
elif not candidate_idx:
    st.warning("Không có hồ sơ nào qua bộ lọc.")
else:
    try:
        ai_results, qvec = ai_search(api_key, query, candidate_idx, top_k)
    except Exception as e:
        msg = str(e)
        if "API key" in msg or "API_KEY" in msg or "400" in msg or "403" in msg:
            st.error("API key không hợp lệ. Kiểm tra lại key trong ô bên trái.")
        elif "429" in msg or "RESOURCE_EXHAUSTED" in msg:
            st.error("Đã chạm giới hạn gói miễn phí. Đợi khoảng 1 phút rồi thử lại.")
        else:
            st.error(f"Lỗi khi gọi Gemini API: {msg}")
        st.stop()

    if compare:
        col_ai, col_kw = st.columns(2)
    else:
        col_ai, col_kw = st.container(), None

    with col_ai:
        st.subheader("🤖 Tìm bằng AI (Gemini embedding)")
        for i, s in ai_results:
            card(PROFILES[i], f"Độ tương đồng: {s:.2f}", "ai")

    if col_kw is not None:
        with col_kw:
            st.subheader("🔤 Tìm theo từ khóa thường")
            kw_results = keyword_search(query, candidate_idx, top_k)
            if not kw_results:
                st.info("Không hồ sơ nào trùng từ khóa.")
            for i, s in kw_results:
                card(PROFILES[i], f"Số từ trùng: {s}", "kw")

    with st.expander("Xem 'tọa độ' của câu tìm kiếm là gì"):
        st.write(f"Câu của bạn được biến thành một dãy **{qvec.shape[0]} con số**. 8 số đầu:")
        st.code(np.round(qvec[:8], 3).tolist())
        st.caption("Hồ sơ nào có dãy số 'gần' dãy này nhất (cosine similarity cao nhất) sẽ được xếp trên cùng.")
