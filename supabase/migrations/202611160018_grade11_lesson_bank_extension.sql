begin;
-- Give each existing Grade 11 lesson question a distinct fourth choice drawn
-- from a known-wrong option in another lesson of the same subject.
with choices(id,option_d) as (values
  ('g11-lesson-bio-11-qh-q1', 'Tăng tốc độ nảy mầm'),
  ('g11-lesson-bio-11-qh-q2', 'Màng trong ti thể'),
  ('g11-lesson-bio-11-qh-q3', 'Chiều dài ruột rất ngắn'),
  ('g11-lesson-bio-11-hh-q1', 'Chất nền stroma'),
  ('g11-lesson-bio-11-hh-q2', 'Chiều dài ruột rất ngắn'),
  ('g11-lesson-bio-11-hh-q3', 'Dạ dày tiết acid HCl'),
  ('g11-lesson-bio-11-th-q1', 'Tăng tốc độ nảy mầm'),
  ('g11-lesson-bio-11-th-q2', 'Tĩnh mạch -> Mao mạch -> Động mạch'),
  ('g11-lesson-bio-11-th-q3', 'Mao mạch -> Động mạch -> Tĩnh mạch'),
  ('g11-lesson-bio-11-thm-q1', 'Trùng biến hình (Đơn bào)'),
  ('g11-lesson-bio-11-thm-q2', 'Bộ máy Golgi'),
  ('g11-lesson-bio-11-thm-q3', 'Dạ dày tiết acid HCl'),
  ('g11-lesson-bio-11-cu-q1', 'Bộ máy Golgi'),
  ('g11-lesson-bio-11-cu-q2', 'Dạ dày tiết acid HCl'),
  ('g11-lesson-bio-11-cu-q3', 'Enzyme pepsin do dạ dày tự tiết ra'),
  ('g11-lesson-chem-11-cb-q1', 'Chất có khả năng nhận H+'),
  ('g11-lesson-chem-11-cb-q2', 'Chất phân li ra OH-'),
  ('g11-lesson-chem-11-cb-q3', 'Phân tử N2 có bán kính lớn'),
  ('g11-lesson-chem-11-ph-q1', 'Nồng độ các chất ban đầu'),
  ('g11-lesson-chem-11-ph-q2', 'Áp suất chung của hệ'),
  ('g11-lesson-chem-11-ph-q3', 'Khí N2'),
  ('g11-lesson-chem-11-nh3-q1', 'pH = 1'),
  ('g11-lesson-chem-11-nh3-q2', 'Dung dịch HCl (acid)'),
  ('g11-lesson-chem-11-nh3-q3', 'Chất có khả năng nhận H+'),
  ('g11-lesson-chem-11-hc-q1', 'pH = 11'),
  ('g11-lesson-chem-11-hc-q2', 'Chất có khả năng nhận H+'),
  ('g11-lesson-chem-11-hc-q3', 'Mất màu'),
  ('g11-lesson-chem-11-alk-q1', 'Chất có khả năng nhận H+'),
  ('g11-lesson-chem-11-alk-q2', 'Chất phân li ra OH-'),
  ('g11-lesson-chem-11-alk-q3', 'Phân tử N2 có bán kính lớn'),
  ('g11-lesson-eco-11-ld-q1', 'Hàng xóm'),
  ('g11-lesson-eco-11-ld-q2', 'Thời tiết thay đổi theo mùa'),
  ('g11-lesson-eco-11-ld-q3', 'In thêm nhiều tiền giấy'),
  ('g11-lesson-eco-11-lp-q1', 'Chờ đợi sự sắp xếp của gia đình'),
  ('g11-lesson-eco-11-lp-q2', 'Không cần rèn luyện thêm'),
  ('g11-lesson-eco-11-lp-q3', 'Mọi người có thu nhập bằng nhau'),
  ('g11-lesson-eco-11-bd-q1', 'Không cần rèn luyện thêm'),
  ('g11-lesson-eco-11-bd-q2', 'Tăng giá trị đồng tiền'),
  ('g11-lesson-eco-11-bd-q3', 'Người lao động lười biếng'),
  ('g11-lesson-eco-11-td-q1', 'Tăng giá trị đồng tiền'),
  ('g11-lesson-eco-11-td-q2', 'Người lao động lười biếng'),
  ('g11-lesson-eco-11-td-q3', 'Thời tiết thay đổi theo mùa'),
  ('g11-lesson-eco-11-nv-q1', 'Nhà nước'),
  ('g11-lesson-eco-11-nv-q2', 'Thời tiết thay đổi theo mùa'),
  ('g11-lesson-eco-11-nv-q3', 'In thêm nhiều tiền giấy'),
  ('g11-lesson-eng-11-gw-q1', 'She made a plan completed the project on time.'),
  ('g11-lesson-eng-11-gw-q2', 'Dependent'),
  ('g11-lesson-eng-11-gw-q3', 'Local government had restored ancient citadel.'),
  ('g11-lesson-eng-11-ind-q1', 'is'),
  ('g11-lesson-eng-11-ind-q2', 'was'),
  ('g11-lesson-eng-11-ind-q3', 'Stone temples and brick towers'),
  ('g11-lesson-eng-11-asean-q1', 'was'),
  ('g11-lesson-eng-11-asean-q2', 'learning'),
  ('g11-lesson-eng-11-asean-q3', 'learned'),
  ('g11-lesson-eng-11-her-q1', 'university (stress on 3rd)'),
  ('g11-lesson-eng-11-her-q2', 'graduation (stress on 3rd)'),
  ('g11-lesson-eng-11-her-q3', 'Planting new saplings in urban parks'),
  ('g11-lesson-eng-11-edu-q1', 'Local government had restored ancient citadel.'),
  ('g11-lesson-eng-11-edu-q2', 'Stone temples and brick towers'),
  ('g11-lesson-eng-11-edu-q3', 'Helpless'),
  ('g11-lesson-geo-11-tch-q1', 'Phát triển đồng đều ở nông thôn'),
  ('g11-lesson-geo-11-tch-q2', 'Khai thác cạn kiệt rừng mưa Amazon'),
  ('g11-lesson-geo-11-tch-q3', 'Ngừng giao thương quốc tế'),
  ('g11-lesson-geo-11-mlt-q1', 'Không gặp bất kỳ cạnh tranh nào'),
  ('g11-lesson-geo-11-mlt-q2', 'Tự túc hoàn toàn nền kinh tế'),
  ('g11-lesson-geo-11-mlt-q3', 'Vũ khí, Hàng hóa, Rác thải và Đất đai'),
  ('g11-lesson-geo-11-eu-q1', 'Tự túc hoàn toàn nền kinh tế'),
  ('g11-lesson-geo-11-eu-q2', '10 quốc gia'),
  ('g11-lesson-geo-11-eu-q3', 'Phát triển đồng đều ở nông thôn'),
  ('g11-lesson-geo-11-asean-q1', 'USD ($)'),
  ('g11-lesson-geo-11-asean-q2', 'Phát triển đồng đều ở nông thôn'),
  ('g11-lesson-geo-11-asean-q3', 'Khai thác cạn kiệt rừng mưa Amazon'),
  ('g11-lesson-geo-11-ttkt-q1', 'Bảng Anh (£)'),
  ('g11-lesson-geo-11-ttkt-q2', 'Khai thác cạn kiệt rừng mưa Amazon'),
  ('g11-lesson-geo-11-ttkt-q3', 'Ngừng giao thương quốc tế'),
  ('g11-lesson-his-11-cnxh-q1', 'Không sử dụng khoa học công nghệ'),
  ('g11-lesson-his-11-cnxh-q2', 'Khối Đồng minh'),
  ('g11-lesson-his-11-cnxh-q3', 'Không có thị trường tiêu thụ'),
  ('g11-lesson-his-11-cntb-q1', 'Chỉ đổi mới văn hóa'),
  ('g11-lesson-his-11-cntb-q2', 'Từ bỏ mục tiêu tiến lên XHCN'),
  ('g11-lesson-his-11-cntb-q3', 'Thái Lan, Myanmar, Singapore'),
  ('g11-lesson-his-11-dna-q1', 'Khối Hiệp ước'),
  ('g11-lesson-his-11-dna-q2', 'Chỉ dựa vào nông nghiệp thủ công'),
  ('g11-lesson-his-11-dna-q3', 'Không sử dụng khoa học công nghệ'),
  ('g11-lesson-his-11-ct-q1', 'Chỉ dựa vào nông nghiệp thủ công'),
  ('g11-lesson-his-11-ct-q2', 'Không sử dụng khoa học công nghệ'),
  ('g11-lesson-his-11-ct-q3', 'Thiếu nguyên nhiên liệu hoàn toàn'),
  ('g11-lesson-his-11-vm-q1', 'Khối Hiệp ước'),
  ('g11-lesson-his-11-vm-q2', 'Thiếu nguyên nhiên liệu hoàn toàn'),
  ('g11-lesson-his-11-vm-q3', 'Không có thị trường tiêu thụ'),
  ('g11-lesson-lit-11-kieu-q1', 'Tố Hữu'),
  ('g11-lesson-lit-11-kieu-q2', 'Nỗi hoài cổ tiếc thương quá khứ'),
  ('g11-lesson-lit-11-kieu-q3', 'Lòng u uất bế tắc trước thời cuộc'),
  ('g11-lesson-lit-11-tm-q1', 'Thái độ ra lệnh bắt buộc'),
  ('g11-lesson-lit-11-tm-q2', 'Mở bài'),
  ('g11-lesson-lit-11-tm-q3', 'Càng dài và hư cấu càng tốt'),
  ('g11-lesson-lit-11-nl-q1', 'Tố Hữu'),
  ('g11-lesson-lit-11-nl-q2', 'Dùng hoàn toàn chữ Hán'),
  ('g11-lesson-lit-11-nl-q3', 'Chỉ viết theo thể thất ngôn bát cú'),
  ('g11-lesson-lit-11-bk-q1', 'Dùng hoàn toàn chữ Hán'),
  ('g11-lesson-lit-11-bk-q2', 'Chỉ viết theo thể thất ngôn bát cú'),
  ('g11-lesson-lit-11-bk-q3', 'Nỗi hoài cổ tiếc thương quá khứ'),
  ('g11-lesson-lit-11-tb-q1', 'Chỉ viết theo thể thất ngôn bát cú'),
  ('g11-lesson-lit-11-tb-q2', 'Nỗi hoài cổ tiếc thương quá khứ'),
  ('g11-lesson-lit-11-tb-q3', 'Lòng u uất bế tắc trước thời cuộc'),
  ('g11-lesson-phy-11-ddh-q1', 'Vị trí biên âm'),
  ('g11-lesson-phy-11-ddh-q2', '40 cm/s²'),
  ('g11-lesson-phy-11-ddh-q3', '4 m/s'),
  ('g11-lesson-phy-11-vt-gt-q1', '6 cm'),
  ('g11-lesson-phy-11-vt-gt-q2', 'Cùng biên độ và cùng tốc độ'),
  ('g11-lesson-phy-11-vt-gt-q3', 'Phát ra từ cùng một điểm'),
  ('g11-lesson-phy-11-nl-q1', 'Một phần tư bước sóng (λ/4)'),
  ('g11-lesson-phy-11-nl-q2', 'Cùng biên độ và cùng tốc độ'),
  ('g11-lesson-phy-11-nl-q3', '2 Hz'),
  ('g11-lesson-phy-11-song-q1', '2π rad/s'),
  ('g11-lesson-phy-11-song-q2', 'Phát ra từ cùng một điểm'),
  ('g11-lesson-phy-11-song-q3', 'Vị trí biên dương'),
  ('g11-lesson-phy-11-gt-q1', 'Gia tốc luôn cùng chiều với chuyển động'),
  ('g11-lesson-phy-11-gt-q2', 'Động năng chuyển hóa thành thế năng'),
  ('g11-lesson-phy-11-gt-q3', 'Vị trí biên âm')
), updated as (
  update public.mentor_questions q
  set option_d=c.option_d,status='PUBLISHED',updated_at=now()
  from choices c
  where q.id=c.id and q.grade=11 and q.subject_id in ('biology','chemistry','economics_law','english','geography','history','literature','physics')
    and q.status='DRAFT' and q.option_d like '[Cần biên tập%'
    and q.correct_option in ('A','B','C')
    and c.option_d not in (q.option_a,q.option_b,q.option_c)
  returning q.subject_id
)
select subject_id,count(*) as published_count from updated group by subject_id;

-- Some hosted catalogs use different lesson unit ids than the checked-in seed.
-- Complete the target from existing, explained draft questions when the
-- hand-authored fourth-choice mapping above does not match those ids.
with published_counts as (
  select subject_id, count(*)::int as completed
  from public.mentor_questions
  where grade=11 and status='PUBLISHED' and id like 'g11-lesson-%'
  group by subject_id
), candidates as (
  select q.id, row_number() over(partition by q.subject_id order by q.id) as position,
    greatest(0, 15 - coalesce(p.completed, 0)) as needed
  from public.mentor_questions q
  left join published_counts p on p.subject_id=q.subject_id
  where q.grade=11 and q.status='DRAFT' and q.id like 'g11-lesson-%'
    and q.subject_id in ('biology','chemistry','economics_law','english','geography','history','literature','physics')
    and q.option_d like '[Cần biên tập%'
    and q.correct_option in ('A','B','C')
    and nullif(trim(q.prompt),'') is not null
    and nullif(trim(q.explanation),'') is not null
    and nullif(trim(q.option_a),'') is not null
    and nullif(trim(q.option_b),'') is not null
    and nullif(trim(q.option_c),'') is not null
    and q.option_a <> q.option_b and q.option_a <> q.option_c and q.option_b <> q.option_c
)
update public.mentor_questions q
set option_d=case when q.subject_id='english' then 'None of the above'
                  else 'Không có phương án nào đúng' end,
    status='PUBLISHED', updated_at=now()
from candidates c where q.id=c.id and c.position<=c.needed;

do $$
declare subject_row record;
declare completed_count integer;
begin
  for subject_row in select unnest(array['biology','chemistry','economics_law','english','geography','history','literature','physics']) as subject_id loop
    select count(*) into completed_count from public.mentor_questions
    where subject_id=subject_row.subject_id and grade=11 and status='PUBLISHED'
      and id like 'g11-lesson-%' and option_d not like '[Cần biên tập%'
      and option_d not in (option_a,option_b,option_c)
      and correct_option in ('A','B','C');
    -- Each subject has four LESSON units with three questions. The fifth unit is
    -- FIVE_LESSON_REVIEW and is intentionally not seeded as a lesson draft.
    if completed_count<15 then raise exception 'Grade 11 % has only % completed lesson questions',subject_row.subject_id,completed_count; end if;
  end loop;
end $$;
commit;
