# 🛠️ SolarTools

**SolarTools** là một plugin công cụ tùy chỉnh (Custom Tools) mạnh mẽ dành cho server Minecraft (Spigot / Paper / Purpur / Folia 1.21+). Plugin hỗ trợ bảo vệ khu vực với **WorldGuard**, đào khối diện rộng theo hướng nhìn, công cụ pháo hoa bứt tốc Elytra vô hạn, và hệ thống hết hạn linh hoạt theo thời gian hoặc số lượt sử dụng.

---

## ✨ Tính Năng Nổi Bật

### 🔨 7 Công Cụ Tùy Chỉnh Chuyên Nghiệp
- **⚡ Mũi Đào (Drill)**: Đào đá/quặng diện rộng 3x3 theo hướng nhìn thực tế.
- **🪓 Rìu Chặt Cây (TreeChopper)**: Tự động đốn hạ toàn bộ cây trồng và tán lá chỉ trong 1 lần chặt.
- **🪵 Xẻng Đào Đất (Shovel)**: Đào đất/dơ/cát diện rộng 3x3 theo hướng nhìn.
- **🌾 Cuốc Đất (Hoe)**: Cày xới đất diện rộng 3x3 trên mặt phẳng ngang.
- **🌊 Xô Nước Vô Hạn (WaterBucket)**: 
  - Đặt nước hoặc **waterlog** (đọng nước) chuẩn Vanilla cho các khối Slab, Cầu thang, Hàng rào,...
  - Tự động **bốc hơi** khi sử dụng tại **Nether** kèm âm thanh & hiệu ứng khói bốc lên.
  - Tự động ngăn chặn dòng nước chảy tràn vào khu vực bảo vệ (WorldGuard Region).
- **🧰 Công Cụ Đa Năng (MultiTool)**: Tích hợp khả năng đào mọi loại khối diện rộng 3x3.
- **🚀 Rocket Vô Hạn (Amethyst Rocket)**:
  - Bứt tốc bay Elytra vô hạn không tiêu hao pháo hoa.
  - Tương thích 100% cơ chế Vanilla: Đang bay Elytra ➔ Bứt tốc | Đứng dưới đất ➔ Bắn/đặt pháo hoa.

---

### ⏳ Chế Độ Hết Hạn Đôi (Dual Expiration Modes)
Mỗi công cụ có thể tùy chỉnh chế độ hết hạn trong `tool/*.yml`:
1. **Time Mode (`expiration-mode: time`)**: Đếm ngược thời gian hết hạn (VD: `3d`, `7d`).
2. **Uses Mode (`expiration-mode: uses`)**: Đếm ngược theo số lần sử dụng (VD: `100u`, `500u`).

---

### 🎨 Bảng Điều Khiển Admin GUI (`/tool`)
- Giao diện GUI đẹp mắt với tiêu đề Small Caps: **`ѕᴏʟᴀʀ ᴛᴏᴏʟѕ`**.
- Cho phép Admin chọn nhanh **Chế Độ Hết Hạn** (`TIME` / `USES`) và **Giá Trị Preset** trực tiếp trên GUI trước khi click lấy tool.

---

## 📜 Lệnh & Quyền Hạn

| Lệnh | Mô tả | Quyền hạn |
| :--- | :--- | :--- |
| `/tool` | Mở giao diện Admin GUI nhận công cụ tùy chỉnh | `solartools.admin` |
| `/givetools <tool> <player> [amount] [duration\|uses]` | Trao công cụ tùy chỉnh cho người chơi | `solartools.admin` |
| `/solartool reload` | Tải lại toàn bộ cấu hình plugin | `solartools.admin` |

### Ví dụ Lệnh:
```bash
/givetools drill Steve 1 3d      # Trao 1 Drill hết hạn sau 3 ngày
/givetools rocket Steve 1 500u   # Trao 1 Rocket hết hạn sau 500 lượt dùng
```

---

## ⚙️ Cấu Hình (Configuration)

Cấu hình chung nằm trong `config.yml`, và mỗi công cụ có file cấu hình riêng nằm trong thư mục `tool/`:
- `tool/drill.yml`
- `tool/treechopper.yml`
- `tool/shovel.yml`
- `tool/hoe.yml`
- `tool/waterbucket.yml`
- `tool/multitool.yml`
- `tool/rocket.yml`

---

## 🛠️ Biên Dịch & Đóng Gói (Build)

### Yêu cầu:
- JDK 21+
- Maven 3.8+

### Lệnh Biên Dịch:
```bash
# Build chuẩn Maven:
mvn clean package
```

---

## 📄 License
Được phát triển bởi **OmhVN**.
