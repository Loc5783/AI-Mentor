"""Original grade-10 cellular biology extension, not a copied textbook."""

LESSONS = [
    ("water", "Nước trong tế bào",
     "Nước có tính phân cực, tạo liên kết hydrogen và hòa tan nhiều chất ion hoặc phân cực. Nước là môi trường của nhiều phản ứng sinh hóa; không phải dung môi hòa tan được mọi chất.",
     "Muối ăn hòa tan trong nước nhưng dầu không hòa tan tốt. Hai hiện tượng liên quan tính chất phân tử và tương tác với nước, không chỉ do chất ở thể rắn hay lỏng.", [
         ("Nước hòa tan tốt nhiều chất nhờ đặc tính nào?", "Tính phân cực", "Hoàn toàn không phân cực", "Không tương tác với ion", "Phân tử nước có các phần điện tích khác nhau, tương tác được với ion và nhiều phân tử phân cực."),
         ("Chất nào không hòa tan tốt trong nước ở điều kiện thường?", "Dầu ăn", "Muối ăn với lượng vừa phải", "Đường với lượng vừa phải", "Dầu chủ yếu gồm các phân tử kị nước, khó hòa tan trong nước phân cực."),
         ("Nhận định nào phù hợp về nước trong tế bào?", "Là môi trường phản ứng nhưng không hòa tan mọi chất", "Chỉ làm tăng khối lượng, không có vai trò khác", "Hòa tan hoàn toàn mọi loại lipid", "Nước tham gia cấu trúc và phản ứng, nhưng các chất kị nước vẫn cần cách tổ chức hoặc vận chuyển riêng.")]),
    ("carbohydrates", "Carbohydrate: đường và chất dự trữ",
     "Carbohydrate gồm đường đơn, đường đôi và đường đa. Glucose là nguồn nguyên liệu năng lượng quan trọng; tinh bột ở thực vật và glycogen ở động vật giúp dự trữ, còn cellulose tham gia thành tế bào thực vật.",
     "Hạt gạo dự trữ tinh bột. Trong cơ thể người, glycogen tập trung chủ yếu ở gan và cơ; cellulose không được enzyme tiêu hóa của người phân giải như tinh bột.", [
         ("Glucose thuộc nhóm nào?", "Đường đơn", "Protein", "Nucleic acid", "Glucose là monosaccharide, một đơn vị đường đơn phổ biến."),
         ("Chất dự trữ carbohydrate điển hình ở động vật là?", "Glycogen", "Cellulose", "DNA", "Glycogen dự trữ glucose, đặc biệt ở gan và cơ."),
         ("Tinh bột và cellulose đều tạo từ glucose nhưng có vai trò khác nhau vì?", "Cách liên kết và cấu trúc khác nhau", "Cellulose không chứa nguyên tử carbon", "Tinh bột là protein", "Kiểu liên kết và cách sắp xếp các đơn vị làm thay đổi hình dạng, tính chất và khả năng enzyme phân giải.")]),
    ("lipids", "Lipid: dự trữ và tạo màng",
     "Lipid là nhóm chất đa dạng, phần lớn ít tan trong nước. Mỡ dự trữ năng lượng; phospholipid có đầu ưa nước và đuôi kị nước, thuận lợi hình thành lớp kép của màng.",
     "Trong lớp kép phospholipid, các đầu ưa nước hướng về môi trường nước hai phía, các đuôi kị nước hướng vào nhau bên trong.", [
         ("Đặc điểm chung của nhiều lipid là?", "Ít tan trong nước", "Đều là chuỗi amino acid", "Đều chứa thông tin di truyền", "Lipid có nhiều vùng không phân cực, nên thường khó hòa tan trong nước."),
         ("Trong lớp kép màng, đuôi phospholipid thường hướng?", "Vào nhau, tránh môi trường nước", "Ra nước ở cả hai phía", "Chỉ vào nhân tế bào", "Đuôi kị nước được che bên trong, còn đầu ưa nước hướng ra các môi trường nước."),
         ("Chức năng nào phù hợp với mỡ dự trữ?", "Dự trữ năng lượng dài hạn", "Làm khuôn trực tiếp sao chép DNA", "Luôn xúc tác mọi phản ứng", "Mỡ là nguồn dự trữ giàu năng lượng; không đảm nhiệm chức năng lưu trữ trình tự di truyền của DNA.")]),
    ("proteins", "Protein và hình dạng chức năng",
     "Protein được cấu tạo từ amino acid nối bằng liên kết peptide. Trình tự và sự gấp cuộn quyết định cấu trúc không gian và chức năng; nhiệt độ hoặc pH không phù hợp có thể gây biến tính.",
     "Lòng trắng trứng đông lại khi đun do protein thay đổi cấu trúc. Biến tính không nhất thiết đồng nghĩa mọi liên kết peptide đều bị cắt.", [
         ("Đơn phân cấu tạo protein là?", "Amino acid", "Glucose", "Nucleotide", "Các amino acid nối thành chuỗi polypeptide qua liên kết peptide."),
         ("Protein có thể mất chức năng khi bị biến tính chủ yếu do?", "Thay đổi cấu trúc không gian phù hợp", "Luôn chuyển thành DNA", "Tự mất mọi nguyên tử carbon", "Nhiều chức năng phụ thuộc hình dạng; thay đổi gấp cuộn có thể làm mất vị trí gắn hay hoạt tính."),
         ("Đun nóng một enzyme làm mất hoạt tính. Kết luận hợp lý nhất?", "Cấu trúc enzyme có thể đã biến đổi", "Mọi amino acid chắc chắn đã biến thành glucose", "Enzyme chưa từng là protein", "Nhiệt cao có thể làm biến tính enzyme protein; chỉ từ mất hoạt tính chưa kết luận mọi liên kết peptide bị phá.")]),
    ("nucleic-acids", "DNA và RNA",
     "DNA và RNA là nucleic acid, cấu tạo từ nucleotide. DNA lưu trữ thông tin di truyền; nhiều loại RNA tham gia biểu hiện thông tin. DNA thường có A, T, G, C; RNA thường có A, U, G, C.",
     "Trong DNA mạch kép, A bắt cặp với T, G với C. Đoạn một mạch A–G–T có mạch bổ sung tương ứng T–C–A, nếu chỉ xét thứ tự bắt cặp.", [
         ("Đơn phân của nucleic acid là?", "Nucleotide", "Amino acid", "Acid béo", "Nucleotide gồm đường, phosphate và base nitrogen."),
         ("Base thường có ở RNA thay cho T của DNA là?", "U", "G", "C", "RNA dùng uracil (U), còn DNA thường dùng thymine (T)."),
         ("Trong DNA mạch kép, mạch A–G–T có các base bổ sung lần lượt?", "T–C–A", "U–C–A", "A–G–T", "A ghép T, G ghép C, T ghép A. U thuộc RNA, không dùng cho câu hỏi DNA này.")]),
    ("organelles", "Sự phối hợp của các bào quan",
     "Ribosome tổng hợp protein; lưới nội chất và bộ máy Golgi tham gia xử lí, vận chuyển nhiều sản phẩm tế bào. Ti thể tham gia hô hấp tế bào; lục lạp quang hợp ở tế bào thực vật có khả năng quang hợp.",
     "Một protein tiết ra ngoài thường được tổng hợp trên ribosome gắn lưới nội chất hạt, đi qua hệ thống màng và Golgi rồi được đưa ra ngoài qua túi vận chuyển.", [
         ("Cấu trúc trực tiếp tổng hợp chuỗi polypeptide là?", "Ribosome", "Bộ máy Golgi", "Thành cellulose", "Ribosome thực hiện dịch mã để tạo chuỗi polypeptide."),
         ("Bào quan nổi bật trong biến đổi, phân loại và đóng gói nhiều sản phẩm là?", "Bộ máy Golgi", "Nucleoid của vi khuẩn", "Thành tế bào", "Golgi nhận và xử lí nhiều sản phẩm từ lưới nội chất trước khi phân phối."),
         ("Protein tiết bị kẹt do hệ Golgi hoạt động kém. Khâu dễ bị ảnh hưởng là?", "Xử lí và phân phối protein", "Nhân đôi mọi neutron", "Tạo ánh sáng cho quang hợp", "Golgi hỗ trợ hoàn thiện, phân loại, đóng gói; rối loạn làm giảm vận chuyển protein đến nơi cần.")]),
    ("enzymes", "Enzyme và trung tâm hoạt động",
     "Enzyme là chất xúc tác sinh học, phần lớn có bản chất protein. Enzyme giảm năng lượng hoạt hóa, có tính đặc hiệu và không bị tiêu hao tổng thể sau phản ứng. Hoạt tính phụ thuộc điều kiện như nhiệt độ, pH.",
     "Amylase hỗ trợ phân giải tinh bột nhưng không thay thế mọi enzyme khác. Tăng nhiệt độ quá cao có thể làm enzyme biến tính thay vì luôn làm phản ứng nhanh hơn.", [
         ("Enzyme làm phản ứng thuận lợi hơn chủ yếu bằng cách?", "Giảm năng lượng hoạt hóa", "Tự tạo thêm nguyên tử", "Luôn tăng nhiệt độ đến sôi", "Enzyme tạo con đường phản ứng có rào năng lượng thấp hơn, không thay đổi quy luật bảo toàn vật chất."),
         ("Một enzyme thường chỉ phù hợp với một số cơ chất vì?", "Đặc điểm trung tâm hoạt động phù hợp", "Nó không tương tác với phân tử nào", "Mọi cơ chất đều có hình dạng giống nhau", "Sự phù hợp cấu trúc và tính chất hóa học giúp enzyme nhận diện cơ chất."),
         ("Đun rất nóng làm tốc độ phản ứng do enzyme giảm. Giải thích phù hợp?", "Enzyme có thể bị biến tính", "Enzyme chắc chắn hoạt động mạnh hơn vô hạn", "Năng lượng hoạt hóa tự bằng 0 mãi mãi", "Nhiệt quá mức làm thay đổi cấu trúc, trung tâm hoạt động có thể không còn phù hợp cơ chất.")]),
    ("atp", "ATP và sự ghép cặp năng lượng",
     "ATP là chất trung gian truyền năng lượng phổ biến trong tế bào, gồm adenine, ribose và ba nhóm phosphate. Thủy phân ATP thành ADP và phosphate có thể ghép với quá trình cần năng lượng; ATP được tái tạo liên tục.",
     "Bơm ion chủ động có thể ghép với thủy phân ATP để đưa ion ngược gradient. ATP không thay thế mỡ hay glycogen như kho dự trữ năng lượng dài hạn chính.", [
         ("ATP có bao nhiêu nhóm phosphate?", "3", "1", "2", "Tên adenosine triphosphate thể hiện ba nhóm phosphate."),
         ("Sản phẩm thường xét khi thủy phân ATP là?", "ADP và phosphate vô cơ", "DNA và glucose", "Protein và cellulose", "Phản ứng ATP + nước → ADP + Pi có thể cung cấp năng lượng tự do cho quá trình ghép cặp."),
         ("Bơm ion vận chuyển ngược gradient cần ATP. ATP chủ yếu đóng vai trò?", "Trung gian ghép năng lượng cho vận chuyển", "Mang nguyên vẹn ion qua màng như túi", "Tạo thêm ion từ hư không", "Bơm dùng năng lượng của phản ứng ghép cặp để đổi cấu hình và vận chuyển ion; ATP không tự là chiếc túi chở ion.")]),
    ("cellular-respiration", "Hô hấp tế bào",
     "Hô hấp tế bào phân giải chất hữu cơ và chuyển một phần năng lượng thành ATP. Trong hô hấp hiếu khí, oxygen là chất nhận electron cuối; đường phân ở tế bào chất, các giai đoạn tiếp theo chủ yếu ở ti thể của tế bào nhân thực.",
     "Tế bào người có thể oxi hóa glucose, tạo CO₂, nước và ATP. Không đồng nhất hô hấp tế bào với động tác hít thở của cả cơ thể.", [
         ("Vai trò quan trọng của hô hấp tế bào là?", "Chuyển năng lượng chất hữu cơ thành dạng tế bào sử dụng", "Chỉ đưa không khí vào phổi", "Luôn tạo glucose từ CO₂", "Hô hấp tế bào là chuỗi phản ứng chuyển hóa; hít thở hỗ trợ trao đổi khí ở cấp cơ thể."),
         ("Ở tế bào nhân thực, đường phân diễn ra tại?", "Tế bào chất", "Chỉ trong nhân", "Ngoài thành tế bào", "Đường phân diễn ra ở bào tương, không bắt buộc phải có ti thể."),
         ("Thiếu oxygen kéo dài, chuỗi truyền electron hiếu khí bị ảnh hưởng vì?", "Thiếu chất nhận electron cuối cùng", "Oxygen là enzyme duy nhất của đường phân", "Mọi tế bào lập tức không còn DNA", "Trong hô hấp hiếu khí, O₂ nhận electron cuối. Thiếu nó làm hạn chế chuỗi truyền electron và tổng hợp ATP theo cơ chế này.")]),
    ("photosynthesis", "Quang hợp ở thực vật",
     "Quang hợp chuyển năng lượng ánh sáng thành năng lượng hóa học, sử dụng CO₂ để tạo chất hữu cơ. Ở thực vật, pha sáng diễn ra ở màng thylakoid; chu trình Calvin diễn ra ở chất nền lục lạp và sử dụng sản phẩm pha sáng.",
     "Oxygen giải phóng trong quang hợp có nguồn từ nước. Cây vẫn hô hấp tế bào cả khi có ánh sáng; quang hợp không thay thế hô hấp.", [
         ("Bào quan quang hợp ở thực vật là?", "Lục lạp", "Ti thể", "Bộ máy Golgi", "Lục lạp chứa hệ sắc tố và bộ máy quang hợp."),
         ("Nguồn carbon để tổng hợp chất hữu cơ trong quang hợp là?", "CO₂", "O₂", "N₂ khí quyển trực tiếp ở mọi cây", "CO₂ được cố định vào hợp chất hữu cơ; O₂ không phải nguồn carbon."),
         ("Ban ngày, tế bào lá có lục lạp thường thực hiện?", "Cả quang hợp và hô hấp tế bào", "Chỉ quang hợp, hô hấp dừng hoàn toàn", "Chỉ hô hấp, không thể quang hợp", "Khi điều kiện phù hợp, quang hợp diễn ra song song với hô hấp tế bào, dù trao đổi khí ròng có thể nghiêng về quang hợp.")]),
    ("cell-cycle", "Chu kì tế bào",
     "Chu kì tế bào gồm kì trung gian và giai đoạn phân bào. Trong kì trung gian, pha S nhân đôi DNA; tế bào còn tăng trưởng và chuẩn bị. Các điểm kiểm soát giúp hạn chế truyền sai hỏng sang tế bào con.",
     "Trước nguyên phân, một nhiễm sắc thể được nhân đôi thành hai chromatid chị em gắn nhau. Lượng DNA tăng nhưng số nhiễm sắc thể tính theo tâm động chưa tăng ngay ở pha S.", [
         ("DNA được nhân đôi chủ yếu ở pha nào của kì trung gian?", "Pha S", "Pha G₁", "Pha G₂", "Pha S là giai đoạn tổng hợp, nhân đôi DNA trước phân bào."),
         ("Hai chromatid chị em hình thành sau quá trình?", "Nhân đôi DNA", "Tế bào mất toàn bộ DNA", "Hô hấp tế bào trực tiếp", "Nhân đôi tạo hai bản sao DNA gắn với nhau trong nhiễm sắc thể kép."),
         ("Điểm kiểm soát phát hiện DNA hỏng trước phân chia giúp?", "Hạn chế truyền sai hỏng cho tế bào con", "Đảm bảo mọi tế bào phân chia nhanh vô hạn", "Thay mọi gene bằng protein", "Dừng chu kì có thể tạo thời gian sửa chữa hoặc ngăn tế bào sai hỏng tiếp tục phân chia.")]),
    ("mitosis", "Nguyên phân và sự phân li",
     "Nguyên phân phân li các chromatid chị em về hai cực sau khi DNA đã nhân đôi. Nếu diễn ra bình thường và chia tế bào chất hoàn tất, một tế bào tạo hai tế bào con có bộ nhiễm sắc thể như tế bào mẹ ban đầu.",
     "Tế bào cơ thể có 2n = 4 trải qua một lần nguyên phân bình thường tạo hai tế bào con, mỗi tế bào 2n = 4.", [
         ("Sau một lần nguyên phân và chia tế bào chất bình thường, số tế bào con là?", "2", "4", "8", "Một tế bào phân chia thành hai tế bào con."),
         ("Tế bào mẹ 2n = 8 nguyên phân bình thường. Mỗi tế bào con có?", "8 nhiễm sắc thể", "4 nhiễm sắc thể", "16 nhiễm sắc thể ổn định", "Nguyên phân bảo toàn số nhiễm sắc thể của tế bào mẹ ở mỗi tế bào con."),
         ("Một tế bào nguyên phân liên tiếp hai lần, mọi tế bào con đều tiếp tục chia bình thường. Có bao nhiêu tế bào?", "4", "2", "8", "Mỗi lần số tế bào gấp đôi: 1 → 2 → 4. Không nhầm số lần phân chia với số tế bào.")]),
    ("meiosis", "Giảm phân và bộ nhiễm sắc thể",
     "Giảm phân gồm một lần nhân đôi DNA và hai lần phân bào, tạo tế bào có số nhiễm sắc thể giảm một nửa. Cặp nhiễm sắc thể tương đồng phân li ở giảm phân I; chromatid chị em thường phân li ở giảm phân II.",
     "Một tế bào mẹ 2n = 6 giảm phân bình thường tạo các sản phẩm mang n = 3. Tổ hợp và trao đổi chéo góp phần tạo biến dị di truyền.", [
         ("Trước hai lần phân bào giảm phân, DNA nhân đôi mấy lần?", "1", "2", "Không nhân đôi", "DNA nhân đôi trước giảm phân I; không có lần nhân đôi DNA xen giữa giảm phân I và II."),
         ("Tế bào mẹ 2n = 10 giảm phân bình thường. Sản phẩm đơn bội có?", "5 nhiễm sắc thể", "10 nhiễm sắc thể", "20 nhiễm sắc thể", "Bộ đơn bội n bằng một nửa 2n, nên n = 5."),
         ("Điểm phân biệt quan trọng ở giảm phân I là?", "Các nhiễm sắc thể tương đồng phân li", "DNA nhân đôi thêm ngay khi hai cực tách", "Mọi tế bào con giữ nguyên bộ lưỡng bội", "Sự phân li của cặp tương đồng làm giảm số bộ nhiễm sắc thể; chromatid chị em chủ yếu tách ở lần II.")]),
    ("microbial-growth", "Sinh trưởng của quần thể vi khuẩn",
     "Nhiều vi khuẩn sinh sản bằng phân đôi. Trong pha lũy thừa với điều kiện phù hợp và thời gian thế hệ g không đổi, N = N₀ × 2ᵏ, k = t/g. Công thức không áp dụng vô hạn khi dinh dưỡng cạn hoặc chất thải tích tụ.",
     "Ban đầu 100 tế bào, sau ba thế hệ phân đôi lý tưởng: N = 100 × 2³ = 800 tế bào.", [
         ("Sau một lần phân đôi bình thường, một vi khuẩn tạo?", "2 tế bào", "4 tế bào ngay lập tức", "1 virus", "Một tế bào mẹ phân chia thành hai tế bào con."),
         ("Ban đầu 20 tế bào, sau hai thế hệ phân đôi lý tưởng có?", "80", "40", "60", "N = 20 × 2² = 80 tế bào."),
         ("Vì sao không dùng N = N₀ × 2ᵏ mãi trong bình nuôi kín?", "Dinh dưỡng và môi trường trở nên hạn chế", "Vi khuẩn không bao giờ cần nguyên liệu", "Số tế bào luôn tự giữ ở 2", "Mô hình tăng lũy thừa cần điều kiện phù hợp; cạn dinh dưỡng và tích tụ chất thải làm thay đổi tốc độ sinh trưởng.")]),
    ("viruses", "Virus và tế bào chủ",
     "Virus có vật chất di truyền DNA hoặc RNA và vỏ protein; một số có vỏ ngoài. Virus không có hệ tế bào và chuyển hóa độc lập, phải dựa vào tế bào chủ để nhân lên. Kháng sinh kháng khuẩn không trực tiếp trị virus.",
     "Virus cúm nhân lên trong tế bào chủ. Dùng kháng sinh cho nhiễm virus đơn thuần không loại được virus; việc điều trị cần hướng dẫn chuyên môn, không tự dùng thuốc.", [
         ("Virus cần yếu tố nào để nhân lên?", "Tế bào chủ phù hợp", "Chỉ nước tinh khiết", "Chỉ đường trong bình vô trùng", "Virus cần bộ máy và nguyên liệu của tế bào chủ vì không có chuyển hóa độc lập đầy đủ."),
         ("Thành phần có ở virus điển hình là?", "Vật chất di truyền và vỏ protein", "Nhân có màng và ti thể", "Ribosome tự hoạt động độc lập", "Virus không có cấu trúc tế bào; hệ gene và capsid là hai thành phần cơ bản."),
         ("Vì sao kháng sinh kháng khuẩn không trực tiếp diệt virus?", "Virus không có nhiều đích tác động đặc trưng của vi khuẩn", "Virus là một vi khuẩn lớn hơn", "Kháng sinh luôn làm virus biến thành tế bào người", "Nhiều kháng sinh nhắm thành tế bào hoặc ribosome vi khuẩn; virus không có các cấu trúc đó. Không suy ra cách điều trị cá nhân từ bài học này.")]),
]
