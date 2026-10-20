"""Original grade-10 mechanics lessons, appended without changing reward identities."""

LESSONS = [
    ("uniform-acceleration", "Chuyển động thẳng biến đổi đều",
     "Khi gia tốc không đổi, vận tốc v = v₀ + at. Công thức dùng vận tốc có dấu trong cùng một hệ quy chiếu; nhanh dần hay chậm dần phải xét cả v và a.",
     "Xe có v₀ = 3 m/s, a = 2 m/s². Sau 4 s, v = 3 + 2 × 4 = 11 m/s.", [
         ("Trong chuyển động thẳng biến đổi đều, đại lượng nào không đổi?", "Gia tốc", "Vận tốc", "Tọa độ", "Biến đổi đều nghĩa vận tốc thay đổi những lượng bằng nhau trong các khoảng thời gian bằng nhau: a không đổi."),
         ("Vật có v₀ = 2 m/s, a = 3 m/s². Sau 2 s, v bằng?", "8 m/s", "6 m/s", "10 m/s", "v = v₀ + at = 2 + 3 × 2 = 8 m/s."),
         ("Xe có v₀ = 12 m/s, a = −3 m/s² không đổi. Sau bao lâu xe dừng lần đầu?", "4 s", "9 s", "36 s", "Đặt v = 0: 0 = 12 − 3t, suy ra t = 4 s. Công thức đang xét đến thời điểm dừng.")]),
    ("motion-displacement", "Độ dịch chuyển khi gia tốc không đổi",
     "Chuyển động thẳng gia tốc không đổi có Δx = v₀t + at²/2. Độ dịch chuyển có thể khác quãng đường nếu vật đổi chiều trong khoảng xét.",
     "Vật xuất phát nghỉ, a = 2 m/s² trong 3 s: Δx = 0 + 2 × 3²/2 = 9 m; vật không đổi chiều nên quãng đường cũng là 9 m.", [
         ("Xuất phát nghỉ, gia tốc không đổi: độ dịch chuyển sau t là?", "at²/2", "at", "a/t", "Thay v₀ = 0 vào Δx = v₀t + at²/2."),
         ("Vật xuất phát nghỉ, a = 4 m/s² trong 2 s. Độ dịch chuyển là?", "8 m", "4 m", "16 m", "Δx = 4 × 2²/2 = 8 m."),
         ("Xe có v₀ = 6 m/s, a = −2 m/s². Đến lúc dừng lần đầu, xe đi được?", "9 m", "18 m", "3 m", "Thời gian dừng t = 6/2 = 3 s. Δx = 6 × 3 − 2 × 3²/2 = 9 m. Chưa đổi chiều nên quãng đường bằng 9 m.")]),
    ("velocity-graph", "Đọc đồ thị vận tốc – thời gian",
     "Trên đồ thị v–t, hệ số góc cho gia tốc. Diện tích có dấu giữa đường biểu diễn và trục thời gian cho độ dịch chuyển; phần dưới trục mang dấu âm.",
     "v tăng đều từ 0 lên 6 m/s trong 3 s. Diện tích tam giác cho Δx = 3 × 6/2 = 9 m; a = 6/3 = 2 m/s².", [
         ("Độ dốc của đồ thị v–t biểu thị?", "Gia tốc", "Khối lượng", "Lực luôn bằng 0", "Độ dốc là Δv/Δt, chính là gia tốc trung bình trên đoạn xét."),
         ("Đồ thị có v = 4 m/s không đổi trong 5 s. Độ dịch chuyển là?", "20 m", "1,25 m", "9 m", "Diện tích hình chữ nhật v × t = 4 × 5 = 20 m."),
         ("Trong 2 s đầu v = +3 m/s, 2 s sau v = −3 m/s. Độ dịch chuyển toàn khoảng là?", "0 m", "12 m", "6 m", "Cộng diện tích có dấu: 3 × 2 + (−3) × 2 = 0 m. Quãng đường là 12 m.")]),
    ("free-fall", "Rơi tự do gần mặt đất",
     "Rơi tự do là chuyển động chỉ chịu trọng lực. Gần mặt đất, bỏ qua sức cản và coi g không đổi; chọn chiều xuống dương thì vật thả nghỉ có v = gt, s = gt²/2.",
     "Lấy g = 10 m/s², thả nghỉ trong 2 s: v = 20 m/s hướng xuống và s = 20 m.", [
         ("Mô hình rơi tự do bỏ qua yếu tố nào?", "Sức cản không khí", "Trọng lực", "Gia tốc rơi", "Vật chỉ chịu trọng lực; khi sức cản đáng kể thì mô hình này không còn phù hợp."),
         ("Thả nghỉ, g = 10 m/s². Sau 1 s, tốc độ là?", "10 m/s", "5 m/s", "20 m/s", "v = gt = 10 × 1 = 10 m/s."),
         ("Thả nghỉ từ độ cao 45 m, g = 10 m/s², bỏ qua sức cản. Thời gian chạm đất là?", "3 s", "4,5 s", "9 s", "45 = 10t²/2 = 5t², nên t² = 9, chọn t = 3 s.")]),
    ("horizontal-launch", "Ném ngang: tách hai phương",
     "Bỏ qua sức cản: phương ngang chuyển động đều, phương đứng rơi tự do. Hai phương dùng cùng thời gian; t = √(2h/g), tầm ngang L = v₀t khi vật được ném ngang từ độ cao h.",
     "Ném ngang ở h = 20 m, v₀ = 5 m/s, g = 10 m/s²: t = 2 s, L = 10 m.", [
         ("Bỏ qua sức cản, vận tốc theo phương ngang của vật ném ngang?", "Không đổi", "Luôn bằng 0", "Tăng do trọng lực", "Trọng lực thẳng đứng nên gia tốc ngang bằng 0."),
         ("Ném ngang v₀ = 6 m/s, thời gian bay 2 s. Tầm ngang là?", "12 m", "3 m", "8 m", "L = v₀t = 6 × 2 = 12 m."),
         ("Hai vật cùng độ cao: một thả nghỉ, một ném ngang cùng lúc. Bỏ qua sức cản, mặt đất phẳng. Vật nào chạm đất trước?", "Cùng lúc", "Vật ném ngang", "Vật thả nghỉ", "Cả hai có vận tốc đứng ban đầu bằng 0 và cùng gia tốc g nên có cùng thời gian rơi.")]),
    ("force-resultant", "Lực và hợp lực",
     "Lực là đại lượng vectơ. Các lực cùng phương cùng chiều cộng độ lớn; ngược chiều lấy hiệu và chiều theo lực lớn hơn. Hợp lực quyết định gia tốc, không phải từng lực riêng lẻ.",
     "Kéo sang phải 8 N, sang trái 3 N: hợp lực 5 N sang phải.", [
         ("Lực có cả độ lớn và chiều nên là đại lượng?", "Vectơ", "Chỉ là số không có đơn vị", "Luôn dương theo mọi trục", "Biểu diễn lực cần độ lớn, phương, chiều và điểm đặt."),
         ("Hai lực cùng chiều có độ lớn 4 N và 6 N. Hợp lực bằng?", "10 N", "2 N", "24 N", "Cùng phương cùng chiều nên F = 4 + 6 = 10 N."),
         ("Hai lực ngang 12 N sang phải, 7 N sang trái. Hợp lực là?", "5 N sang phải", "19 N sang trái", "5 N sang trái", "Chọn chiều phải dương: F = 12 − 7 = +5 N, tức sang phải.")]),
    ("inertia", "Quán tính và định luật I Newton",
     "Trong hệ quy chiếu quán tính, khi hợp lực bằng 0, vật giữ nguyên trạng thái đứng yên hoặc chuyển động thẳng đều. Không cần lực cùng chiều vận tốc để duy trì chuyển động thẳng đều.",
     "Xe phanh, cơ thể có xu hướng tiếp tục chuyển động về trước do quán tính; dây an toàn tạo lực giúp người giảm tốc cùng xe.", [
         ("Hợp lực bằng 0, một vật đang chuyển động sẽ?", "Tiếp tục thẳng đều trong hệ quán tính", "Dừng ngay", "Tự tăng tốc", "Định luật I: vận tốc không đổi khi hợp lực bằng 0 trong hệ quán tính."),
         ("Sách nằm yên trên bàn: trọng lực và lực đỡ có hợp lực?", "Bằng 0", "Bằng hai lần trọng lực", "Luôn hướng xuống", "Hai lực cân bằng theo phương đứng nên sách không có gia tốc."),
         ("Vì sao hành khách nghiêng về trước khi xe phanh?", "Cơ thể có xu hướng giữ vận tốc trước đó", "Có lực bí ẩn luôn đẩy người về trước", "Trọng lực đột ngột biến mất", "Do quán tính, cơ thể chưa giảm vận tốc ngay như xe; dây an toàn giúp giảm tốc an toàn.")]),
    ("newton-second", "Định luật II Newton",
     "Trong hệ quán tính, hợp lực F = ma đối với vật có khối lượng không đổi. Gia tốc cùng chiều hợp lực; đơn vị N = kg·m/s².",
     "Hợp lực ngang 6 N tác dụng lên vật 2 kg: a = 6/2 = 3 m/s² theo chiều hợp lực.", [
         ("Trong F = ma, F là?", "Hợp lực tác dụng lên vật", "Chỉ lực lớn nhất", "Luôn là trọng lực", "Phải cộng vectơ tất cả lực để tìm hợp lực trước khi tính gia tốc."),
         ("Hợp lực 8 N, khối lượng 4 kg. Gia tốc bằng?", "2 m/s²", "32 m/s²", "0,5 m/s²", "a = F/m = 8/4 = 2 m/s²."),
         ("Vật 2 kg chịu lực 10 N sang phải và 4 N sang trái. Gia tốc là?", "3 m/s² sang phải", "7 m/s² sang phải", "3 m/s² sang trái", "Hợp lực 10 − 4 = 6 N sang phải; a = 6/2 = 3 m/s².")]),
    ("newton-third", "Lực và phản lực",
     "Hai vật tương tác tác dụng lên nhau hai lực cùng phương, ngược chiều, bằng độ lớn. Hai lực đặt lên hai vật khác nhau nên không phải cặp lực cân bằng trên một vật.",
     "Tay đẩy tường 20 N thì tường đẩy tay 20 N theo chiều ngược lại, dù tường không chuyển động thấy rõ.", [
         ("Lực và phản lực tác dụng lên?", "Hai vật khác nhau", "Cùng một vật", "Chỉ vật nặng hơn", "Định luật III mô tả hai lực của cùng tương tác nhưng đặt trên hai vật."),
         ("Chân đẩy đất về sau khi đi bộ. Đất tác dụng lên chân theo chiều nào?", "Về trước", "Về sau", "Không có lực", "Đất tác dụng lực ma sát về trước lên chân, đối ứng lực chân đẩy đất về sau."),
         ("Xe tải và xe nhỏ va chạm. Lực tương tác hai xe trong cùng thời điểm thế nào?", "Bằng độ lớn, ngược chiều", "Xe tải luôn tác dụng lực lớn hơn", "Xe nhỏ không tác dụng lực", "Lực bằng nhau theo định luật III; gia tốc và biến dạng có thể khác do khối lượng và cấu trúc khác.")]),
    ("weight-normal", "Trọng lực và lực đỡ",
     "Trọng lực gần mặt đất có độ lớn P = mg, hướng xuống. Lực đỡ vuông góc mặt tiếp xúc. N = mg chỉ đúng trong những điều kiện cân bằng đứng phù hợp, không phải mọi tình huống.",
     "Sách 0,5 kg nằm yên trên bàn ngang, chỉ có trọng lực và lực đỡ theo phương đứng. Lấy g = 10 m/s²: P = N = 5 N.", [
         ("Công thức độ lớn trọng lực gần mặt đất là?", "P = mg", "P = m/g", "P = g/m", "Với g tại nơi xét, trọng lực có độ lớn bằng khối lượng nhân gia tốc trọng trường."),
         ("Vật 3 kg, g = 10 m/s². Trọng lực bằng?", "30 N", "3 N", "0,3 N", "P = 3 × 10 = 30 N."),
         ("Ấn thêm lực 4 N xuống sách đang yên trên bàn ngang. Sách có P = 6 N. Lực đỡ bằng?", "10 N", "6 N", "2 N", "Cân bằng đứng: N − 6 − 4 = 0, nên N = 10 N; không thể mặc định N = P.")]),
    ("sliding-friction", "Ma sát trượt",
     "Trong mô hình đơn giản, ma sát trượt F = μN, ngược chiều trượt tương đối giữa hai mặt. Hệ số μ không có đơn vị; công thức này không mô tả mọi trường hợp ma sát nghỉ.",
     "Vật trượt trên bàn, N = 20 N và μ = 0,2: ma sát trượt 4 N, ngược chiều trượt.", [
         ("Ma sát trượt trên vật có chiều?", "Ngược chiều trượt tương đối", "Luôn cùng chiều trượt", "Luôn thẳng đứng", "Cần xét chuyển động tương đối tại chỗ tiếp xúc, không chỉ hướng chuyển động so với đất."),
         ("μ = 0,3; N = 10 N. Ma sát trượt có độ lớn?", "3 N", "30 N", "0,03 N", "F = μN = 0,3 × 10 = 3 N."),
         ("Vật đang trượt sang phải, kéo 9 N sang phải, ma sát 3 N sang trái, m = 2 kg. Gia tốc là?", "3 m/s² sang phải", "6 m/s² sang phải", "3 m/s² sang trái", "Hợp lực ngang 9 − 3 = 6 N, a = 6/2 = 3 m/s² sang phải.")]),
    ("work", "Công của lực không đổi",
     "Công A = Fs cosα khi lực không đổi, α là góc giữa lực và độ dịch chuyển thẳng. Công dương hỗ trợ chuyển động, âm cản chuyển động; lực vuông góc độ dịch chuyển có công bằng 0.",
     "Kéo vật đi 3 m bằng lực 5 N cùng chiều: A = 5 × 3 = 15 J.", [
         ("Đơn vị SI của công là?", "Joule (J)", "Watt (W)", "Newton (N)", "1 J = 1 N·m; watt là đơn vị công suất."),
         ("Lực 4 N cùng chiều dịch chuyển 2 m. Công bằng?", "8 J", "2 J", "16 J", "α = 0°, cosα = 1, nên A = 4 × 2 = 8 J."),
         ("Lực ma sát 2 N ngược chiều vật trượt 5 m. Công của ma sát là?", "−10 J", "+10 J", "0 J", "Góc 180° có cosα = −1, nên A = 2 × 5 × (−1) = −10 J.")]),
    ("power", "Công suất",
     "Công suất trung bình bằng công thực hiện chia thời gian: P = A/t. Công suất lớn nghĩa truyền năng lượng nhanh hơn, không nhất thiết tổng năng lượng lớn hơn.",
     "Máy thực hiện công 600 J trong 3 s có công suất trung bình 200 W.", [
         ("Công suất đo tốc độ thực hiện?", "Công hoặc truyền năng lượng", "Chỉ quãng đường", "Khối lượng", "Công suất là năng lượng truyền trong một đơn vị thời gian."),
         ("Thực hiện 1000 J trong 5 s. Công suất trung bình là?", "200 W", "5000 W", "5 W", "P = 1000/5 = 200 W."),
         ("Hai máy cùng thực hiện 1200 J. Máy A mất 4 s, máy B mất 6 s. Nhận xét đúng?", "A có công suất lớn hơn B", "B có công suất lớn hơn A", "Hai máy công suất bằng nhau", "P_A = 300 W; P_B = 200 W. Cùng công nhưng A thực hiện nhanh hơn.")]),
    ("kinetic-potential", "Động năng và thế năng trọng trường",
     "Động năng Wđ = mv²/2. Thế năng trọng trường Wt = mgh gần mặt đất, với h tính từ mốc chọn; thế năng phụ thuộc mốc nhưng độ biến thiên giữa hai vị trí không phụ thuộc mốc.",
     "Vật 2 kg chạy 3 m/s có Wđ = 9 J. Cũng vật đó ở cao 1 m so với mốc, g = 10 m/s²: Wt = 20 J.", [
         ("Giữ m không đổi, tốc độ tăng gấp đôi thì động năng?", "Tăng gấp bốn", "Tăng gấp đôi", "Không đổi", "Động năng tỉ lệ v² nên (2v)² = 4v²."),
         ("Vật 2 kg chuyển động 4 m/s. Động năng bằng?", "16 J", "8 J", "32 J", "Wđ = 2 × 4²/2 = 16 J."),
         ("Vật 1 kg được nâng từ h = 1 m lên h = 3 m, g = 10 m/s². Thế năng tăng bao nhiêu?", "20 J", "30 J", "10 J", "ΔWt = mg(h₂ − h₁) = 1 × 10 × (3 − 1) = 20 J.")]),
    ("mechanical-energy", "Bảo toàn cơ năng",
     "Cơ năng bằng tổng động năng và thế năng. Khi chỉ trọng lực sinh công, cơ năng bảo toàn. Có ma sát sinh công thì một phần cơ năng chuyển thành nội năng; không thể áp dụng bảo toàn cơ năng máy móc.",
     "Thả nghỉ từ h = 5 m, g = 10 m/s², không có sức cản: mgh = mv²/2 tại đất, nên v = √(2gh) = 10 m/s.", [
         ("Cơ năng trong trọng trường là tổng?", "Động năng và thế năng trọng trường", "Khối lượng và tốc độ", "Công suất và thời gian", "Hai dạng năng lượng cơ học có thể chuyển hóa lẫn nhau."),
         ("Cơ năng 30 J, thế năng 12 J. Động năng bằng?", "18 J", "42 J", "2,5 J", "Wđ = W − Wt = 30 − 12 = 18 J."),
         ("Vật thả nghỉ từ h = 20 m, g = 10 m/s², bỏ qua sức cản. Tốc độ ngay trước chạm đất là?", "20 m/s", "10 m/s", "40 m/s", "Bảo toàn cơ năng: v = √(2gh) = √400 = 20 m/s, lấy độ lớn dương.")]),
]
