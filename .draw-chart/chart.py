import matplotlib.pyplot as plt
import numpy as np
import csv
import os

# ==========================================
# 1. NHẬP SỐ LIỆU CỦA BẠN VÀO ĐÂY (Thay số thật)
# ==========================================
scenarios = ['50 Users\n(Baseline)', '200 Users\n(Load)', '500 Users\n(Stress)']

# Số liệu từ K6 Summary (Ví dụ mẫu, hãy thay bằng số của bạn)
avg_times   = [13.15, 33.68, 629.54]   # Đơn vị: ms
p95_times   = [15.52, 48.58, 5898.50]  # Đơn vị: ms
throughputs = [7.2, 128.5, 178.6]      # Đơn vị: req/s
error_rates = [0.0, 3.0, 13.89]        # Đơn vị: %

# Tên file log tài nguyên (tạo ra từ monitor.py)
resource_file = 'resource_log.csv'

# ==========================================
# 2. CODE VẼ BIỂU ĐỒ (KHÔNG CẦN SỬA)
# ==========================================

# --- CHART 1: RESPONSE TIME (Độ trễ) ---
def draw_chart_1():
    x = np.arange(len(scenarios))
    width = 0.35

    fig, ax = plt.subplots(figsize=(10, 6))
    rects1 = ax.bar(x - width/2, avg_times, width, label='Trung bình (Avg)', color='#4CAF50')
    rects2 = ax.bar(x + width/2, p95_times, width, label='95% Users (P95)', color='#FF9800')

    ax.set_ylabel('Thời gian phản hồi (ms)')
    ax.set_title('Hình 1: Độ trễ hệ thống qua các mức tải')
    ax.set_xticks(x)
    ax.set_xticklabels(scenarios)
    ax.legend()
    ax.grid(axis='y', linestyle='--', alpha=0.5)

    # Ghi số liệu lên cột
    for rect in rects1 + rects2:
        height = rect.get_height()
        ax.annotate(f'{int(height)}', xy=(rect.get_x() + rect.get_width() / 2, height),
                    xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=9)

    plt.tight_layout()
    plt.savefig('Chart1_ResponseTime.png', dpi=300)
    print("✅ Đã vẽ xong Chart 1: Chart1_ResponseTime.png")

# --- CHART 2: THROUGHPUT vs ERROR (Sự ổn định) ---
def draw_chart_2():
    fig, ax1 = plt.subplots(figsize=(10, 6))

    color = 'tab:blue'
    ax1.set_xlabel('Kịch bản kiểm thử')
    ax1.set_ylabel('Thông lượng (Req/s)', color=color, fontweight='bold')
    ax1.bar(scenarios, throughputs, color=color, alpha=0.6, width=0.5, label='Throughput')
    ax1.tick_params(axis='y', labelcolor=color)

    ax2 = ax1.twinx()  # Trục Y thứ 2
    color = 'tab:red'
    ax2.set_ylabel('Tỷ lệ lỗi (%)', color=color, fontweight='bold')
    ax2.plot(scenarios, error_rates, color=color, marker='o', linewidth=3, markersize=8, label='Error Rate')
    ax2.tick_params(axis='y', labelcolor=color)
    ax2.set_ylim(0, max(error_rates) + 5) # Tăng giới hạn trục y một chút

    # Ghi % lỗi
    for i, v in enumerate(error_rates):
        ax2.text(i, v + 0.5, f"{v}%", ha='center', color='red', fontweight='bold', bgcolor='white')

    plt.title('Hình 2: Tương quan giữa Tải xử lý và Tỷ lệ lỗi')
    plt.tight_layout()
    plt.savefig('Chart2_Stability.png', dpi=300)
    print("✅ Đã vẽ xong Chart 2: Chart2_Stability.png")

# --- CHART 3: RESOURCE OVER TIME (Tài nguyên theo thời gian) ---
def draw_chart_3():
    if not os.path.exists(resource_file):
        print(f"⚠️ Không tìm thấy file {resource_file}. Hãy chạy monitor.py trước để vẽ Chart 3!")
        return

    times, cpus, rams = [], [], []
    try:
        with open(resource_file, 'r') as csvfile:
            plots = csv.reader(csvfile, delimiter=',')
            next(plots) # Bỏ header
            for row in plots:
                times.append(int(row[0]))
                cpus.append(float(row[1]))
                rams.append(float(row[2]))
    except Exception as e:
        print(f"Lỗi đọc file CSV: {e}")
        return

    fig, ax1 = plt.subplots(figsize=(12, 6))

    # Vẽ CPU (Trục trái)
    color = '#D32F2F' # Đỏ
    ax1.set_xlabel('Thời gian test (giây)')
    ax1.set_ylabel('CPU Usage (%)', color=color, fontweight='bold')
    ax1.plot(times, cpus, color=color, linewidth=2, label='CPU')
    ax1.tick_params(axis='y', labelcolor=color)
    ax1.fill_between(times, cpus, color=color, alpha=0.1) # Tô màu nền cho đẹp
    ax1.grid(True, linestyle='--', alpha=0.5)

    # Vẽ RAM (Trục phải)
    ax2 = ax1.twinx()
    color = '#1976D2' # Xanh
    ax2.set_ylabel('RAM Usage (MB)', color=color, fontweight='bold')
    ax2.plot(times, rams, color=color, linewidth=2, linestyle='--', label='RAM')
    ax2.tick_params(axis='y', labelcolor=color)

    plt.title('Hình 3: Biến thiên tài nguyên hệ thống (Stress Test 500 VUs)')
    fig.legend(loc="upper left", bbox_to_anchor=(0.1, 0.9))

    plt.tight_layout()
    plt.savefig('Chart3_Resources.png', dpi=300)
    print("✅ Đã vẽ xong Chart 3: Chart3_Resources.png")

# --- CHẠY TẤT CẢ ---
draw_chart_1()
draw_chart_2()
draw_chart_3()
print("\n🎉 Xong! Hãy kiểm tra thư mục hiện tại để lấy 3 file ảnh.")