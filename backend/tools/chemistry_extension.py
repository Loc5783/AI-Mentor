"""Original grade-10 chemistry extension; each block has five short lessons."""

LESSONS = [
    ("electron-shells", "Lớp và phân lớp electron",
     "Electron trong nguyên tử được mô tả theo lớp và phân lớp năng lượng. Lớp n có tối đa 2n² electron; các phân lớp s, p, d, f chứa tối đa 2, 6, 10, 14 electron. Sức chứa tối đa không đồng nghĩa lớp đã được điền đầy.",
     "Lớp n = 2 có phân lớp 2s và 2p, tổng sức chứa tối đa 2 + 6 = 8 electron.", [
         ("Phân lớp s chứa tối đa bao nhiêu electron?", "2", "6", "8", "Phân lớp s có một orbital; mỗi orbital chứa tối đa hai electron."),
         ("Lớp n = 2 chứa tối đa bao nhiêu electron?", "8", "4", "18", "2n² = 2 × 2² = 8 electron."),
         ("Phân lớp 2p đang có 4 electron. Nó nhận thêm tối đa bao nhiêu để đầy?", "2", "4", "6", "Phân lớp p chứa tối đa 6; còn 6 − 4 = 2 chỗ electron.")]),
    ("electron-configuration", "Viết cấu hình electron đơn giản",
     "Ở trạng thái cơ bản, electron điền vào các mức năng lượng thấp trước. Với 18 nguyên tố đầu có thứ tự 1s, 2s, 2p, 3s, 3p. Tổng các số mũ bằng số electron của nguyên tử hoặc ion đang xét.",
     "Na trung hòa có Z = 11: 1s² 2s² 2p⁶ 3s¹. Tổng electron là 2 + 2 + 6 + 1 = 11.", [
         ("Tổng số mũ trong cấu hình electron cho biết?", "Số electron", "Luôn là số neutron", "Luôn là số khối", "Mỗi số mũ đếm electron ở phân lớp tương ứng."),
         ("Cấu hình 1s² 2s² 2p⁶ có tổng bao nhiêu electron?", "10", "8", "6", "Cộng 2 + 2 + 6 = 10 electron."),
         ("Nguyên tử Mg có Z = 12. Cấu hình trạng thái cơ bản nào đúng?", "1s² 2s² 2p⁶ 3s²", "1s² 2s² 2p⁸", "1s² 2s² 2p⁶ 3s¹", "Mg trung hòa có 12 electron; 2p tối đa 6 và hai electron còn lại vào 3s.")]),
    ("valence-electrons", "Electron lớp ngoài cùng",
     "Electron lớp ngoài cùng thuộc lớp có n lớn nhất đang có electron. Với nguyên tố nhóm A, chúng giúp dự đoán nhiều tính chất hóa học; không được suy rộng mọi quy tắc nhóm A cho kim loại chuyển tiếp.",
     "Al: 1s² 2s² 2p⁶ 3s² 3p¹. Lớp ngoài cùng n = 3 có 2 + 1 = 3 electron.", [
         ("Lớp ngoài cùng là lớp đang có electron với n?", "Lớn nhất", "Nhỏ nhất", "Luôn bằng 1", "Xét lớp có electron và số lượng tử chính n lớn nhất."),
         ("Na có cấu hình kết thúc 3s¹, không có electron lớp cao hơn. Số electron ngoài cùng là?", "1", "3", "11", "Lớp ngoài cùng n = 3 chỉ chứa một electron ở 3s."),
         ("Cấu hình kết thúc 3s² 3p⁵, không có lớp cao hơn. Electron ngoài cùng là?", "7", "5", "17", "Cộng electron cùng lớp n = 3: 2 + 5 = 7; không chỉ đếm phân lớp cuối.")]),
    ("periodic-table", "Chu kì trong bảng tuần hoàn",
     "Bảng tuần hoàn hiện đại xếp nguyên tố theo chiều tăng số hiệu nguyên tử Z. Với nguyên tử trạng thái cơ bản, số lớp có electron bằng số thứ tự chu kì.",
     "Na có ba lớp electron và Z = 11 nên thuộc chu kì 3; vị trí không được xác định bằng số neutron.", [
         ("Bảng tuần hoàn xếp theo chiều tăng của?", "Số hiệu nguyên tử", "Số neutron", "Số khối của mọi đồng vị", "Z là số proton và xác định nguyên tố; các đồng vị vẫn chung một ô."),
         ("Nguyên tử trạng thái cơ bản có hai lớp electron thuộc chu kì?", "2", "1", "4", "Số lớp đang có electron bằng số chu kì."),
         ("Mg có cấu hình 1s² 2s² 2p⁶ 3s². Thuộc chu kì nào?", "3", "2", "12", "Có các lớp n = 1, 2, 3; lớp lớn nhất n = 3 nên ở chu kì 3.")]),
    ("periodic-trends", "Xu hướng tính chất trong một chu kì",
     "Trong một chu kì của các nguyên tố nhóm A, từ trái sang phải bán kính nguyên tử nhìn chung giảm; độ âm điện nhìn chung tăng nếu không xét khí hiếm. Đây là xu hướng, không thay thế dữ liệu khi cần so sánh chính xác.",
     "Trong chu kì 3, Na có bán kính nguyên tử lớn hơn Cl; Cl hút electron liên kết mạnh hơn Na.", [
         ("Từ trái sang phải trong một chu kì nhóm A, bán kính nhìn chung?", "Giảm", "Tăng mãi", "Luôn bằng nhau", "Điện tích hạt nhân tăng trong khi electron được thêm cùng lớp chính, làm lực hút hiệu dụng nhìn chung tăng."),
         ("Trong chu kì 3, Na và Cl: nguyên tử nào thường có bán kính lớn hơn?", "Na", "Cl", "Luôn bằng nhau", "Na ở phía trái chu kì, phù hợp xu hướng bán kính giảm sang phải."),
         ("So Na và Cl, nguyên tố nào hút cặp electron liên kết mạnh hơn?", "Cl", "Na", "Không thể so bằng độ âm điện", "Cl có độ âm điện cao hơn; độ âm điện mô tả khả năng hút electron trong liên kết.")]),
    ("octet", "Quy tắc octet và giới hạn",
     "Nhiều nguyên tử nhóm A có xu hướng nhường, nhận hoặc dùng chung electron để có lớp ngoài cùng bền kiểu khí hiếm, thường tám electron. H thường đạt hai electron; quy tắc octet không đúng cho mọi chất.",
     "Na nhường một electron tạo Na⁺ có cấu hình như Ne. Cl nhận một electron tạo Cl⁻ có cấu hình như Ar.", [
         ("Theo quy tắc octet, nhiều nguyên tử hướng đến bao nhiêu electron ngoài cùng?", "8", "12", "18", "Octet nghĩa là tám; vẫn có ngoại lệ và H theo cấu hình hai electron."),
         ("Na có một electron ngoài cùng thường tạo Na⁺ bằng cách?", "Nhường 1 electron", "Nhận 1 electron", "Nhường 1 proton", "Mất một electron tạo điện tích +1; hạt nhân không đổi trong quá trình hóa học này."),
         ("Trong phân tử H₂, mỗi H có thể được xem là đạt lớp bền với?", "2 electron nhờ cặp dùng chung", "8 electron riêng", "0 electron", "Hai H dùng chung một cặp electron; mỗi H đạt cấu hình lớp thứ nhất hai electron, không phải octet.")]),
    ("ionic-bond", "Liên kết ion",
     "Liên kết ion là lực hút tĩnh điện giữa các ion trái dấu. Chất ion thường tạo mạng tinh thể; công thức biểu diễn tỉ lệ ion để tổng điện tích bằng 0, không nhất thiết là phân tử riêng biệt.",
     "Mg²⁺ cần hai Cl⁻ để trung hòa điện: công thức MgCl₂.", [
         ("Liên kết ion hình thành chủ yếu do?", "Hút tĩnh điện giữa ion trái dấu", "Hai ion cùng dấu hút nhau", "Chuyển neutron qua lại", "Ion dương và ion âm hút nhau bằng tương tác điện."),
         ("Ca²⁺ và Cl⁻ tạo chất có công thức nào?", "CaCl₂", "Ca₂Cl", "CaCl", "Một Ca²⁺ cần hai Cl⁻ để tổng điện tích +2 − 2 = 0."),
         ("Al³⁺ và O²⁻ tạo chất trung hòa với tỉ lệ ion tối giản nào?", "2 Al³⁺ : 3 O²⁻", "1 Al³⁺ : 1 O²⁻", "3 Al³⁺ : 2 O²⁻", "Bội chung của 3 và 2 là 6: hai ion Al tổng +6, ba ion O tổng −6, được Al₂O₃.")]),
    ("covalent-bond", "Liên kết cộng hóa trị",
     "Liên kết cộng hóa trị được tạo bởi một hoặc nhiều cặp electron dùng chung. Một cặp tạo liên kết đơn, hai cặp tạo liên kết đôi, ba cặp tạo liên kết ba.",
     "Trong H₂, hai nguyên tử H dùng chung một cặp electron, tạo liên kết đơn H–H.", [
         ("Đặc trưng của liên kết cộng hóa trị là?", "Cặp electron dùng chung", "Luôn chuyển hoàn toàn electron thành ion", "Dùng chung proton", "Các nguyên tử liên kết thông qua một hay nhiều cặp electron chung."),
         ("Liên kết đôi tương ứng bao nhiêu cặp electron chung?", "2", "1", "4", "Mỗi vạch liên kết biểu diễn một cặp electron dùng chung."),
         ("Trong N₂ có liên kết ba. Số electron tham gia ba cặp chung là?", "6", "3", "12", "Ba cặp, mỗi cặp hai electron: 3 × 2 = 6 electron dùng chung.")]),
    ("bond-polarity", "Độ phân cực của liên kết",
     "Khi hai nguyên tử hút cặp electron chung khác nhau, liên kết có thể phân cực về nguyên tử có độ âm điện lớn hơn. Phân cực liên kết và phân cực toàn phân tử là hai khái niệm khác nhau.",
     "Trong H–Cl, electron chung lệch về Cl: Cl mang phần điện tích âm, H mang phần điện tích dương; không phải hai ion tách rời trong phân tử khí HCl.", [
         ("Cặp electron chung lệch về nguyên tử có?", "Độ âm điện lớn hơn", "Số neutron luôn lớn hơn", "Khối lượng luôn nhỏ hơn", "Độ âm điện mô tả khả năng hút electron của nguyên tử trong liên kết."),
         ("Liên kết Cl–Cl giữa hai nguyên tử giống nhau được coi là?", "Cộng hóa trị không phân cực", "Ion thuần túy", "Không có electron chung", "Hai nguyên tử có cùng độ âm điện nên không có sự lệch ưu tiên của cặp chung."),
         ("CO₂ thẳng có hai liên kết C=O phân cực đối xứng. Toàn phân tử?", "Không phân cực do các moment triệt tiêu", "Luôn phân cực vì từng liên kết phân cực", "Không có liên kết cộng hóa trị", "Cần xét hình học: hai moment bằng nhau ngược chiều nên tổng moment lưỡng cực bằng 0.")]),
    ("hydrogen-bond", "Liên kết hydrogen",
     "Liên kết hydrogen thường xuất hiện khi H đã liên kết với F, O hoặc N tương tác với cặp electron tự do thích hợp của nguyên tử khác. Tương tác này yếu hơn liên kết cộng hóa trị O–H bên trong phân tử nước.",
     "Giữa các phân tử nước có liên kết hydrogen; đun sôi chủ yếu làm các phân tử tách xa nhau, không phân hủy nước thành hydrogen và oxygen.", [
         ("Trong nước, liên kết hydrogen điển hình hình thành giữa?", "Các phân tử nước", "Hai neutron trong hạt nhân", "Chỉ hai nguyên tử O tạo liên kết đôi", "H của một phân tử tương tác với cặp electron tự do trên O của phân tử khác."),
         ("Liên kết O–H bên trong phân tử nước là?", "Liên kết cộng hóa trị", "Liên kết hydrogen giữa hai phân tử", "Liên kết kim loại", "Phải phân biệt cặp electron chung O–H với tương tác hydrogen giữa các phân tử."),
         ("Khi nước sôi ở điều kiện thường, mô tả phù hợp là?", "Phân tử nước tách xa, vẫn chủ yếu là H₂O", "Mọi H₂O tách thành H₂ và O₂", "Proton biến thành electron", "Chuyển thể không đồng nghĩa phản ứng phân hủy; liên kết cộng hóa trị trong H₂O vẫn được giữ.")]),
    ("oxidation-number", "Xác định số oxi hóa",
     "Tổng số oxi hóa của các nguyên tử bằng điện tích tiểu phân. Đơn chất có số oxi hóa 0; O thường −2 trừ ngoại lệ như peroxide, H thường +1 trừ hydride kim loại. Số oxi hóa là quy ước, không luôn là điện tích thật.",
     "Trong CO₂ trung hòa: x + 2 × (−2) = 0, nên carbon có số oxi hóa +4.", [
         ("Số oxi hóa của nguyên tố trong đơn chất O₂ là?", "0", "−2", "+2", "Mọi nguyên tố trong đơn chất có số oxi hóa bằng 0."),
         ("Trong MgO, O có số oxi hóa −2. Mg có số oxi hóa?", "+2", "−2", "0", "Tổng bằng 0 nên x − 2 = 0, x = +2."),
         ("Trong ion SO₄²⁻, O có số oxi hóa −2. S có số oxi hóa?", "+6", "+8", "+4", "x + 4 × (−2) = −2, nên x = +6. Tổng bằng điện tích ion, không phải 0.")]),
    ("redox", "Oxi hóa và khử",
     "Oxi hóa là nhường electron, số oxi hóa tăng. Khử là nhận electron, số oxi hóa giảm. Chất khử nhường electron và bị oxi hóa; chất oxi hóa nhận electron và bị khử.",
     "Zn → Zn²⁺ + 2e là oxi hóa. Cu²⁺ + 2e → Cu là khử; trong phản ứng Zn + Cu²⁺ → Zn²⁺ + Cu, Zn là chất khử.", [
         ("Quá trình nhường electron gọi là?", "Oxi hóa", "Khử", "Thẩm thấu", "Nhường electron làm số oxi hóa tăng."),
         ("Cu²⁺ nhận 2 electron thành Cu. Cu²⁺ trải qua quá trình?", "Khử", "Oxi hóa", "Không đổi số oxi hóa", "Số oxi hóa giảm từ +2 xuống 0, tương ứng nhận electron."),
         ("Trong Zn + Cu²⁺ → Zn²⁺ + Cu, chất khử là?", "Zn", "Cu²⁺", "Zn²⁺", "Zn nhường electron cho Cu²⁺, nên Zn là chất khử và bị oxi hóa.")]),
    ("reaction-enthalpy", "Phản ứng tỏa nhiệt và thu nhiệt",
     "Phản ứng tỏa nhiệt truyền nhiệt ra môi trường, có ΔH < 0 khi xét hệ phản ứng ở áp suất không đổi. Phản ứng thu nhiệt nhận nhiệt từ môi trường, ΔH > 0. Cần phân biệt hệ và môi trường.",
     "Một phản ứng có ΔH = −100 kJ theo phương trình đã viết: hệ tỏa 100 kJ khi phản ứng xảy ra đúng lượng chất theo hệ số đó.", [
         ("Phản ứng tỏa nhiệt có ΔH?", "Âm", "Dương", "Luôn bằng 0", "Hệ mất năng lượng dưới dạng nhiệt nên enthalpy giảm, ΔH < 0."),
         ("Phản ứng có ΔH = +50 kJ là?", "Thu nhiệt", "Tỏa nhiệt", "Không trao đổi nhiệt", "Dấu dương cho biết hệ nhận nhiệt theo phương trình đã xét."),
         ("Đảo chiều phương trình có ΔH = −80 kJ, cùng lượng chất. ΔH chiều ngược bằng?", "+80 kJ", "−80 kJ", "0 kJ", "Đảo chiều biến đổi thì đổi dấu enthalpy: từ tỏa 80 kJ thành thu 80 kJ.")]),
    ("reaction-rate", "Tốc độ phản ứng",
     "Tốc độ phản ứng thể hiện mức thay đổi nồng độ theo thời gian. Nhiệt độ, nồng độ, diện tích tiếp xúc và chất xúc tác có thể ảnh hưởng tốc độ; không đồng nhất phản ứng nhanh với tạo nhiều sản phẩm cuối hơn.",
     "Cùng khối lượng CaCO₃, dạng bột phản ứng với acid nhanh hơn dạng viên trong điều kiện giống nhau vì diện tích tiếp xúc lớn hơn.", [
         ("Tốc độ phản ứng cho biết?", "Phản ứng diễn ra nhanh hay chậm", "Chỉ khối lượng sản phẩm cuối", "Chỉ số neutron", "Tốc độ liên hệ lượng chất hoặc nồng độ biến đổi trong một đơn vị thời gian."),
         ("Cùng khối lượng đá vôi, dạng bột thường phản ứng nhanh hơn viên do?", "Diện tích tiếp xúc lớn hơn", "Bột luôn có số mol lớn hơn", "Bột không có liên kết hóa học", "Chia nhỏ tăng diện tích tiếp xúc, còn số mol giữ nguyên nếu cùng chất và khối lượng."),
         ("Thêm xúc tác thích hợp vào phản ứng, nhận xét nào đúng?", "Có thể tăng tốc độ mà không bị tiêu hao tổng thể", "Luôn tăng lượng sản phẩm cực đại bất kể chất đầu", "Biến mất hoàn toàn như chất phản ứng", "Xúc tác tạo con đường phản ứng thuận lợi hơn và được tái tạo; không tự tạo thêm nguyên tử hay thay giới hạn chất phản ứng.")]),
    ("halogens", "Làm quen với nhóm halogen",
     "Halogen thuộc nhóm 17 (VIIA), có bảy electron lớp ngoài cùng. Đơn chất thường gồm phân tử X₂ và có tính oxi hóa; ion halide X⁻ nhận thêm một electron so với nguyên tử trung hòa. Không tự thử với halogen vì có độc tính.",
     "Cl₂ + 2e → 2Cl⁻: mỗi nguyên tử chlorine nhận một electron, số oxi hóa giảm từ 0 xuống −1.", [
         ("Nguyên tử halogen nhóm A có bao nhiêu electron ngoài cùng?", "7", "1", "8", "Halogen thuộc nhóm VIIA, tương ứng bảy electron ngoài cùng."),
         ("Cl trung hòa có Z = 17. Ion Cl⁻ có bao nhiêu electron?", "18", "16", "17", "Cl⁻ nhận thêm một electron so với Cl trung hòa: 17 + 1 = 18."),
         ("Trong Cl₂ + 2e → 2Cl⁻, chlorine trải qua quá trình?", "Khử vì nhận electron", "Oxi hóa vì nhận electron", "Không thay đổi số oxi hóa", "Số oxi hóa giảm từ 0 xuống −1; nhận electron là khử.")]),
]
