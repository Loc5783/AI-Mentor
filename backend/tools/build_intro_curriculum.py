"""Build original introductory content, never synthetic quiz fixtures or full-year claims.

Stable unit IDs are reward identities: do not rename them merely to update wording.
Run from any directory: python backend/tools/build_intro_curriculum.py
"""
import json
from pathlib import Path

from biology_extension import LESSONS as BIOLOGY_EXTENSION
from chemistry_extension import LESSONS as CHEMISTRY_EXTENSION
from physics_extension import LESSONS as PHYSICS_EXTENSION

PACKS = {
    ("physics", 10): ("Chuyển động quanh ta", [
        ("position", "Vị trí và hệ quy chiếu",
         "Mô tả vị trí cần một mốc và chiều dương. Tọa độ có thể âm; điều đó không có nghĩa vật không chuyển động.",
         "Chọn cổng trường làm gốc, chiều đông là chiều dương. Một bạn cách cổng 20 m về tây có tọa độ −20 m.", [
             ("Muốn nói rõ vị trí của một vật, trước hết cần chọn gì?", "Mốc và hệ tọa độ", "Chỉ khối lượng vật", "Chỉ màu sắc vật", "Vị trí được xác định tương đối với mốc trong hệ tọa độ đã chọn."),
             ("Gốc tại nhà, chiều đông dương. Bạn ở 30 m về tây có tọa độ bao nhiêu?", "−30 m", "+30 m", "0 m", "Phía tây ngược chiều dương nên tọa độ là −30 m."),
             ("Xe đứng yên so với mặt đường. Người đi bộ có thấy xe đổi vị trí so với mình không?", "Có thể có", "Không bao giờ", "Chỉ khi xe tăng khối lượng", "Chuyển động có tính tương đối. Người đi bộ đổi vị trí so với xe, nên xe cũng đổi vị trí trong hệ quy chiếu gắn với người đó.")]),
        ("displacement", "Quãng đường và độ dịch chuyển",
         "Quãng đường là tổng chiều dài đường đi. Độ dịch chuyển nối vị trí đầu với vị trí cuối; trong chuyển động thẳng, Δx = x₂ − x₁.",
         "Đi 5 m về đông rồi 2 m về tây: quãng đường 7 m, độ dịch chuyển +3 m nếu chọn chiều đông dương.", [
             ("Đại lượng nào bằng tổng chiều dài đường đã đi?", "Quãng đường", "Độ dịch chuyển luôn có dấu", "Gia tốc", "Quãng đường cộng chiều dài từng chặng, không trừ chặng đi ngược chiều."),
             ("Từ x = −2 m đến x = 4 m, độ dịch chuyển bằng bao nhiêu?", "+6 m", "+2 m", "−6 m", "Δx = 4 − (−2) = +6 m."),
             ("Chạy trọn một vòng sân dài 400 m về đúng chỗ cũ, độ lớn độ dịch chuyển là?", "0 m", "400 m", "800 m", "Vị trí đầu và cuối trùng nhau nên độ dịch chuyển bằng 0; quãng đường vẫn là 400 m.")]),
        ("speed", "Tốc độ trung bình",
         "Tốc độ trung bình bằng tổng quãng đường chia tổng thời gian: v = s/t. Tính cả thời gian dừng nếu nó thuộc khoảng thời gian đang xét.",
         "Đi 120 m trong 60 s rồi nghỉ 20 s: tốc độ trung bình của toàn bộ 80 s là 120/80 = 1,5 m/s.", [
             ("Công thức tính tốc độ trung bình là?", "Tổng quãng đường / tổng thời gian", "Độ dịch chuyển × thời gian", "Quãng đường × thời gian", "Tốc độ trung bình dùng tổng quãng đường và tổng thời gian, không lấy trung bình cộng tùy ý các tốc độ."),
             ("Đi được 150 m trong 30 s, tốc độ trung bình là?", "5 m/s", "0,2 m/s", "4500 m/s", "v = s/t = 150/30 = 5 m/s."),
             ("Đi 100 m trong 50 s rồi nghỉ 50 s. Tốc độ trung bình cả khoảng là?", "1 m/s", "2 m/s", "4 m/s", "Tổng thời gian là 100 s. v = 100/100 = 1 m/s.")]),
        ("velocity", "Vận tốc và chiều chuyển động",
         "Vận tốc trung bình bằng độ dịch chuyển chia thời gian. Trong chuyển động thẳng, dấu vận tốc thể hiện chiều so với chiều dương.",
         "Trong 4 s, vật từ x = 10 m đến x = 2 m: v = (2 − 10)/4 = −2 m/s.", [
             ("Vận tốc trung bình được tính từ đại lượng nào?", "Độ dịch chuyển và thời gian", "Chỉ quãng đường", "Chỉ khối lượng", "Vận tốc trung bình là Δx/Δt trong chuyển động thẳng."),
             ("Vật dịch chuyển −12 m trong 3 s. Vận tốc trung bình bằng?", "−4 m/s", "+4 m/s", "−36 m/s", "v = −12/3 = −4 m/s, tức theo chiều âm."),
             ("Đi rồi quay lại đúng vị trí ban đầu sau 20 s. Vận tốc trung bình là?", "0 m/s", "Luôn dương", "Không thể bằng 0", "Độ dịch chuyển bằng 0 nên vận tốc trung bình bằng 0, dù tốc độ trung bình có thể khác 0.")]),
        ("acceleration", "Gia tốc trong chuyển động thẳng",
         "Gia tốc trung bình a = (v₂ − v₁)/Δt. Dấu gia tốc phải xét cùng dấu vận tốc để biết vật nhanh lên hay chậm đi.",
         "Xe có v tăng từ 2 lên 8 m/s trong 3 s: a = (8 − 2)/3 = 2 m/s².", [
             ("Đơn vị SI của gia tốc là?", "m/s²", "m/s", "m²/s", "Gia tốc là độ biến thiên vận tốc trong một giây nên có đơn vị (m/s)/s = m/s²."),
             ("Vận tốc tăng từ 4 lên 10 m/s trong 2 s. Gia tốc trung bình bằng?", "3 m/s²", "7 m/s²", "12 m/s²", "a = (10 − 4)/2 = 3 m/s²."),
             ("Xe đang có v = +10 m/s và a = −2 m/s² không đổi. Ngay lúc đó xe thế nào?", "Đang chậm dần", "Đang nhanh dần", "Đã đứng yên", "Gia tốc ngược dấu vận tốc nên độ lớn vận tốc đang giảm; xe chưa đứng yên khi v vẫn là +10 m/s.")]),
    ]),
    ("chemistry", 10): ("Khám phá nguyên tử", [
        ("particles", "Các hạt trong nguyên tử",
         "Nguyên tử gồm hạt nhân và các electron. Proton mang điện dương, electron mang điện âm; neutron không mang điện.",
         "Nguyên tử carbon-12 có 6 proton, 6 neutron và 6 electron khi trung hòa.", [
             ("Hạt nào mang điện tích âm?", "Electron", "Proton", "Neutron", "Electron mang điện tích −1 theo đơn vị điện tích nguyên tố."),
             ("Hai loại hạt chủ yếu tạo nên hạt nhân nguyên tử là?", "Proton và neutron", "Electron và proton", "Electron và neutron", "Hạt nhân gồm proton và neutron; electron phân bố ngoài hạt nhân."),
             ("Một nguyên tử trung hòa có 8 proton. Nó có bao nhiêu electron?", "8", "16", "0", "Trung hòa điện nghĩa tổng điện tích dương và âm cân bằng. Vì vậy số electron bằng số proton, bằng 8.")]),
        ("atomic-number", "Số hiệu nguyên tử",
         "Số hiệu nguyên tử Z bằng số proton. Các nguyên tử thuộc cùng một nguyên tố có cùng Z.",
         "Mọi nguyên tử oxygen có Z = 8, tức đều có 8 proton, dù số neutron có thể khác nhau.", [
             ("Số hiệu nguyên tử Z bằng số hạt nào?", "Proton", "Neutron", "Proton cộng neutron", "Z được định nghĩa là số proton trong hạt nhân."),
             ("Nguyên tử có 11 proton thuộc nguyên tố có Z bằng?", "11", "22", "Không xác định", "Z bằng số proton nên Z = 11."),
             ("Hai nguyên tử có cùng số proton nhưng khác số neutron có cùng nguyên tố không?", "Có", "Không", "Chỉ khi cùng số khối", "Số proton quyết định nguyên tố. Khác số neutron không làm đổi nguyên tố.")]),
        ("mass-number", "Số khối",
         "Số khối A = Z + N, trong đó N là số neutron. A là số đếm nucleon, không phải khối lượng nguyên tử đo bằng gam.",
         "Natri-23 có Z = 11 nên N = 23 − 11 = 12 neutron.", [
             ("Công thức liên hệ số khối A, proton Z và neutron N là?", "A = Z + N", "A = Z − N", "A = số electron", "Số khối là tổng số proton và neutron trong hạt nhân."),
             ("Nguyên tử có 6 proton và 7 neutron có số khối là?", "13", "1", "6", "A = 6 + 7 = 13."),
             ("Hạt nhân có A = 35 và Z = 17. Số neutron là?", "18", "52", "17", "N = A − Z = 35 − 17 = 18.")]),
        ("isotopes", "Đồng vị",
         "Đồng vị của cùng nguyên tố có cùng số proton nhưng khác số neutron, nên thường khác số khối.",
         "Carbon-12 và carbon-14 đều có 6 proton; chúng lần lượt có 6 và 8 neutron.", [
             ("Các đồng vị của cùng nguyên tố luôn giống nhau về?", "Số proton", "Số neutron", "Số khối", "Cùng nguyên tố nghĩa cùng Z, do đó cùng số proton."),
             ("Carbon-14 có 6 proton. Số neutron là?", "8", "6", "14", "N = 14 − 6 = 8."),
             ("Hai nguyên tử có (Z, A) là (8, 16) và (8, 18). Chúng là?", "Hai đồng vị của oxygen", "Hai nguyên tố khác nhau", "Cùng số neutron", "Cả hai có Z = 8 nhưng A khác nhau, nên là đồng vị; số neutron lần lượt là 8 và 10.")]),
        ("ions", "Nguyên tử và ion",
         "Khi nhường hoặc nhận electron, nguyên tử trở thành ion. Nhường electron tạo ion dương; nhận electron tạo ion âm. Hạt nhân không đổi trong quá trình này.",
         "Na có 11 proton. Khi nhường 1 electron, Na⁺ còn 10 electron và vẫn có 11 proton.", [
             ("Nguyên tử nhường một electron thường tạo thành?", "Ion dương", "Ion âm", "Nguyên tố mới", "Mất electron âm khiến tổng điện tích trở nên dương; số proton không đổi."),
             ("Nguyên tử có 17 proton nhận thêm 1 electron từ trạng thái trung hòa. Ion có bao nhiêu electron?", "18", "16", "17", "Ban đầu có 17 electron; nhận thêm 1 thành 18 electron."),
             ("Ion Mg²⁺ có Z = 12. Số proton và electron lần lượt là?", "12 và 10", "10 và 12", "12 và 14", "Z = 12 nên có 12 proton. Điện tích 2+ do mất 2 electron, còn 10 electron.")]),
    ]),
    ("biology", 10): ("Làm quen với tế bào", [
        ("cell-basics", "Tế bào — đơn vị của sự sống",
         "Tế bào là đơn vị cấu trúc và chức năng cơ bản của cơ thể sống. Một sinh vật có thể gồm một hoặc nhiều tế bào.",
         "Vi khuẩn là sinh vật đơn bào. Cơ thể người gồm nhiều loại tế bào đảm nhiệm các chức năng khác nhau.", [
             ("Đơn vị cấu trúc và chức năng cơ bản của cơ thể sống là?", "Tế bào", "Nguyên tử riêng lẻ", "Cơ quan", "Các hoạt động sống cơ bản diễn ra ở cấp tế bào; cơ quan được cấu tạo từ nhiều tế bào."),
             ("Sinh vật đơn bào có cơ thể gồm?", "Một tế bào", "Đúng hai tế bào", "Không có tế bào", "Đơn bào nghĩa là cơ thể gồm một tế bào thực hiện các hoạt động sống."),
             ("Tế bào cơ và tế bào thần kinh có hình dạng khác nhau chủ yếu vì?", "Chuyên hóa cho chức năng khác nhau", "Không có điểm chung nào", "Một loại không phải tế bào", "Sự chuyên hóa về cấu trúc giúp tế bào đảm nhiệm chức năng như co cơ hoặc truyền tín hiệu.")]),
        ("membrane", "Màng sinh chất",
         "Màng sinh chất bao quanh tế bào, gồm lớp kép phospholipid và protein. Màng có tính thấm chọn lọc, điều hòa trao đổi chất với môi trường.",
         "Một số phân tử nhỏ không phân cực có thể đi qua lớp lipid; nhiều chất khác cần protein vận chuyển.", [
             ("Thành phần nền tảng của màng sinh chất là?", "Lớp kép phospholipid và protein", "Một lớp cellulose duy nhất", "Chỉ DNA", "Lớp kép phospholipid tạo nền màng, protein tham gia vận chuyển và nhiều chức năng khác."),
             ("Tính thấm chọn lọc nghĩa là?", "Các chất đi qua màng với khả năng khác nhau", "Mọi chất đi qua như nhau", "Không chất nào đi qua", "Màng không phải hàng rào kín tuyệt đối; cấu trúc màng và protein quyết định khả năng đi qua của từng chất."),
             ("Nếu màng mất khả năng điều hòa trao đổi chất, nguy cơ trực tiếp là?", "Mất ổn định môi trường bên trong tế bào", "Tự tăng gấp đôi DNA ngay", "Tạo thêm thành cellulose ngay", "Trao đổi không được kiểm soát làm xáo trộn thành phần bên trong, ảnh hưởng hoạt động sống của tế bào.")]),
        ("prokaryote", "Tế bào nhân sơ",
         "Tế bào nhân sơ không có nhân được bao bởi màng nhân. Vật chất di truyền nằm ở vùng nhân; tế bào vẫn có màng sinh chất và ribosome.",
         "Vi khuẩn có DNA ở vùng nhân và ribosome để tổng hợp protein; không có ti thể có màng bao như tế bào nhân thực.", [
             ("Đặc điểm nổi bật của tế bào nhân sơ là?", "Không có nhân được màng nhân bao bọc", "Không có DNA", "Không có màng sinh chất", "Nhân sơ có DNA nhưng DNA không nằm trong nhân có màng bao."),
             ("Cấu trúc nào có ở tế bào vi khuẩn điển hình?", "Ribosome", "Lục lạp có màng kép", "Nhân có màng bao", "Ribosome tham gia tổng hợp protein và có ở cả tế bào nhân sơ lẫn nhân thực."),
             ("Quan sát thấy tế bào không có màng nhân. Có thể kết luận tế bào không có vật chất di truyền không?", "Không; cần phân biệt DNA với màng nhân", "Có; DNA chỉ tồn tại trong màng nhân", "Có; mọi tế bào đều có nhân thực", "Tế bào nhân sơ chứa DNA ở vùng nhân. Thiếu màng nhân không đồng nghĩa thiếu DNA.")]),
        ("eukaryote", "Tế bào nhân thực",
         "Tế bào nhân thực có nhân được màng bao bọc và các bào quan chuyên hóa. Nhân chứa phần lớn DNA của tế bào; ribosome tổng hợp protein.",
         "Tế bào động vật điển hình có nhân, ti thể và ribosome. Tế bào thực vật quang hợp còn có lục lạp.", [
             ("Tế bào nhân thực điển hình có đặc điểm nào?", "Có nhân được màng bao bọc", "Luôn không có DNA", "Không có ribosome", "Nhân có màng bao là đặc điểm phân biệt cơ bản của tế bào nhân thực."),
             ("Cấu trúc trực tiếp tổng hợp protein là?", "Ribosome", "Màng nhân", "Không bào", "Ribosome liên kết các amino acid theo thông tin được truyền từ gene."),
             ("Một tế bào lá đang quang hợp cần bào quan nào trực tiếp thu nhận năng lượng ánh sáng?", "Lục lạp", "Ribosome", "Nhân", "Lục lạp chứa sắc tố quang hợp và bộ máy chuyển hóa năng lượng ánh sáng.")]),
        ("transport", "Vận chuyển qua màng",
         "Khuếch tán thụ động không cần tế bào cung cấp năng lượng trực tiếp và đi theo gradient nồng độ của chất. Thẩm thấu là sự di chuyển của nước qua màng thấm chọn lọc.",
         "Với màng chỉ cho nước qua, nước có xu hướng đi từ phía có nồng độ chất tan thấp sang phía có nồng độ chất tan cao hơn.", [
             ("Vận chuyển thụ động thường có đặc điểm nào?", "Không cần cung cấp năng lượng trực tiếp", "Luôn tiêu thụ ATP trực tiếp", "Luôn ngược gradient nồng độ", "Khuếch tán sử dụng chênh lệch nồng độ, không cần tế bào cung cấp năng lượng trực tiếp."),
             ("Thẩm thấu là sự di chuyển của chất nào qua màng thấm chọn lọc?", "Nước", "Chỉ protein", "Chỉ DNA", "Thuật ngữ thẩm thấu mô tả chuyển động của nước qua màng thấm chọn lọc."),
             ("Màng chỉ cho nước qua, một phía nhiều chất tan hơn. Nước có xu hướng đi về đâu?", "Phía có nồng độ chất tan cao hơn", "Phía có nồng độ chất tan thấp hơn", "Không di chuyển dù có chênh lệch", "Trong điều kiện đã nêu, chênh lệch thế nước làm nước đi về phía có nhiều chất tan hơn. Cần xét cả áp suất nếu điều kiện thay đổi.")]),
    ]),
}


EXTENSIONS = {
    ("physics", 10): ("Cơ học: chuyển động, lực và năng lượng", PHYSICS_EXTENSION),
    ("chemistry", 10): ("Từ nguyên tử đến phản ứng hóa học", CHEMISTRY_EXTENSION),
    ("biology", 10): ("Tế bào, chuyển hóa và sinh sản", BIOLOGY_EXTENSION),
}


def build():
    target = Path(__file__).resolve().parents[1] / "curriculum"
    target.mkdir(exist_ok=True)
    for (subject, grade), (title, units) in PACKS.items():
        title, extension = EXTENSIONS[(subject, grade)]
        # Append only: existing option rotation and IDs must remain unchanged.
        units = units + extension
        lessons = []
        for number, (slug, name, guidance, example, questions) in enumerate(units):
            lid = f"{subject}-{grade}-{slug}"
            values = []
            for qn, (prompt, correct, wrong1, wrong2, explanation) in enumerate(questions):
                choices = [correct, wrong1, wrong2]
                # Stable shuffled positions avoid every answer being the first option.
                shift = (number + qn) % 3
                choices = choices[shift:] + choices[:shift]
                options = [{"id": chr(97 + n), "text": text} for n, text in enumerate(choices)]
                values.append({"id": f"{lid}-q{qn + 1}", "prompt": prompt,
                               "difficulty": ("EASY", "MEDIUM", "APPLIED")[qn], "options": options,
                               "correctOptionId": next(o["id"] for o in options if o["text"] == correct),
                               "explanation": explanation})
            lessons.append({"id": lid, "title": name, "guidance": [guidance], "examples": [example], "questions": values})
        course = {"subjectId": subject, "grade": grade, "version": "intro-2026.1", "title": title,
                  "published": True, "coverage": "PARTIAL_GRADE_10", "extendedReviews": False,
                  "sourceNote": "20 bài ngắn lớp 10 do AI Mentor biên soạn, không phải bản sao SGK hay toàn bộ chương trình năm học. Đầu vào chỉ khảo sát 5 bài nhập môn. Cần giáo viên duyệt trước khi dùng chính thức.",
                  "lessons": lessons}
        (target / f"{subject}-{grade}.json").write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    build()
