# Nạp seed kiểm thử vào MariaDB của docker compose (service "db").
#   powershell scripts/seed/run-flow-seed.ps1            # seed luồng flow-*@example.test
#   powershell scripts/seed/run-flow-seed.ps1 demo004    # seed demo cho Phụ huynh/Gia sư Demo 004
#   powershell scripts/seed/run-flow-seed.ps1 tutors     # thêm 80 gia sư demo đã duyệt (seed.tutor101..180)
#   powershell scripts/seed/run-flow-seed.ps1 activity   # dàn mốc thời gian dữ liệu demo trong 7 ngày
#   powershell scripts/seed/run-flow-seed.ps1 featured   # Phạm Đức Huy (Demo 004) nổi bật, đứng đầu mọi danh sách
param([ValidateSet('flow', 'demo004', 'tutors', 'activity', 'featured')][string]$Seed = 'flow')
$files = @{
  flow    = 'scripts\seed\flow-test-seed.sql'
  demo004 = 'scripts\seed\demo004-seed.sql'
  tutors  = 'scripts\seed\more-tutors-seed.sql'
  activity = 'scripts\seed\activity-spread-seed.sql'
  featured = 'scripts\seed\featured-tutor004-seed.sql'
}
$file = $files[$Seed]
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Push-Location $root
try {
  cmd /c "docker compose exec -T db sh -c ""mariadb -u`$MARIADB_USER -p`$MARIADB_PASSWORD `$MARIADB_DATABASE --default-character-set=utf8mb4"" < $file"
  if ($LASTEXITCODE -ne 0) { throw "Nạp seed thất bại" }
  Write-Host "Đã nạp $file - xem docs/seed-flow-test.md"
} finally { Pop-Location }
