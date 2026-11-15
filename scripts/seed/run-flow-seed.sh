#!/bin/sh
# Nạp seed kiểm thử vào MariaDB của docker compose (service "db"). Chạy từ thư mục gốc dự án.
#   sh scripts/seed/run-flow-seed.sh            # seed luồng flow-*@example.test
#   sh scripts/seed/run-flow-seed.sh demo004    # seed demo cho Phụ huynh Demo 004 + Phạm Đức Huy (Demo 004)
#   sh scripts/seed/run-flow-seed.sh tutors     # thêm 80 gia sư demo đã duyệt (seed.tutor101..180)
#   sh scripts/seed/run-flow-seed.sh activity   # dàn mốc thời gian dữ liệu demo trong 7 ngày (cho trang Giám sát)
#   sh scripts/seed/run-flow-seed.sh featured   # Phạm Đức Huy (Demo 004) nổi bật: luôn đứng đầu Khám phá và Gợi ý AI
set -eu
cd "$(dirname "$0")/../.."
case "${1:-flow}" in
  flow) file=scripts/seed/flow-test-seed.sql ;;
  demo004) file=scripts/seed/demo004-seed.sql ;;
  tutors) file=scripts/seed/more-tutors-seed.sql ;;
  activity) file=scripts/seed/activity-spread-seed.sql ;;
  featured) file=scripts/seed/featured-tutor004-seed.sql ;;
  *) echo "Dùng: $0 [flow|demo004|tutors|activity|featured]" >&2; exit 2 ;;
esac
docker compose exec -T db sh -c 'mariadb -u"$MARIADB_USER" -p"$MARIADB_PASSWORD" "$MARIADB_DATABASE" --default-character-set=utf8mb4' < "$file"
echo "Đã nạp $file — xem docs/seed-flow-test.md"
