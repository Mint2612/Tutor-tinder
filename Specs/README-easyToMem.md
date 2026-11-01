- Có 3 người dùng: admin, tutor - gia sư và tutee - học viên

Database - class của user ( trừ admin ) có gì ?

user chung:

password+username

Bắt buộc not full
1. Full Name char 
2. Role bool (tutor is yes)
3. Gender bool (male is yes)
4. Age ( tutor: > 18 )  num
5. Location: {Provine: enum -> Town: enum } vd: tphcm (1) -> di an (2) 
6. Self-Description: text
7. Contact-info: text (suggest phone, address, gmail ) text
12. {IsUniver:bool--yes-->major:enum---> grade: enum 1,2,3,4
                  ---no-->subject:enum-> grade: enum 1->12
Chỉ riêng tutor: 

8. Self-face: url 
9. Over18-evidence: url 
10. Study-evidence {picture} 

Chỉ riêng tutee:

11. {IsUniver:bool--yes-->major:enum---> grade: enum 1,2,3,4
                  ---no-->subject:enum-> grade: enum 1->12
vd: yes,cs(1),1 -> university studying cs in grade 1.
vd: no, 4       -> children in grade 4

Tutor-public-profile: 1-> 10 (excule 9)
Tutee-public-profile: 1->6 + 11

--------------------------------------------------
REMEMBER: LOCATION ->PROVINE --> SEPERATE ROOM
VD: TPHCM ROOM 1, TUTOR AND TUTEE ONLY FIND OTHER IN SAME PROVINE

Góc nhìn mentee: 

1. Đăng kí (pass+username) --> ( điền 1->5 trong db ) bằng form --> có thể sửa từ sau đăng nhập
2. Trang chính: form add thông tin từ 6->7
3. Trang chính: Tìm kiếm gia sư 
3-1. (ghi chú: có thể thêm reddis cache sau này ) --> bộ lọc --> 
 
send: (Isuniver: bool, major/subject: enum , tuitee-profile)
receive: list (tuitee-public-profile) same major/subject

4. Main: AI, use self-description to find each other


Góc nhìn tutor: 

1. Đăng kí --> (điền 1->5 trong db) bằng form
2. Trang chính: header có chỗ thêm thông tin ( điền 6-> 10)
3. FIND MENTEE
SEND: (ISUNIVER, MAJOR/SUJECT:ENUM, PROFILE)
RECEIVE LIST PROFILE SAME MAJOR
4. AI TO SAME SELF-DES