package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;
import com.tutormatch.dto.recommendation.MatchingDtos.*;
import com.tutormatch.exception.BusinessException;
import com.tutormatch.model.*;
import com.tutormatch.repository.*;
import com.tutormatch.repository.MatchingRepository.State;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingService {
  static final Map<String,String> LEARNING = Map.ofEntries(
      Map.entry("foundation","Củng cố nền tảng"),Map.entry("exam","Chuẩn bị thi"),
      Map.entry("confidence","Tự tin hơn"),Map.entry("visual","Học qua hình ảnh"),
      Map.entry("practice","Thực hành"),Map.entry("discussion","Trao đổi"),
      Map.entry("understanding","Hiểu bản chất"),Map.entry("attention","Duy trì tập trung"),
      Map.entry("motivation","Tăng động lực học"),Map.entry("step_by_step","Hướng dẫn từng bước"),
      Map.entry("balanced","Nhịp học cân bằng"),Map.entry("challenge","Thích thử thách"),
      Map.entry("support","Hướng dẫn sát sao"),Map.entry("independent","Khuyến khích tự lập"),
      Map.entry("feedback","Phản hồi thường xuyên"),Map.entry("examples","Ví dụ thực tế"),
      Map.entry("review","Ôn tập và hệ thống"));
  private final MatchingRepository matching;
  private final StudentRepository studentRepository;
  private final TutorRepository tutorRepository;
  private final StudentService students;
  private final TutorService tutors;
  private final EligibilityPolicy eligibility;
  private final SwipeRepository legacy;
  private final PairAiRepository aiScores;

  public MatchingService(MatchingRepository matching, StudentRepository studentRepository,
      TutorRepository tutorRepository, StudentService students, TutorService tutors,
      EligibilityPolicy eligibility, SwipeRepository legacy, PairAiRepository aiScores) {
    this.matching=matching; this.studentRepository=studentRepository; this.tutorRepository=tutorRepository;
    this.students=students; this.tutors=tutors; this.eligibility=eligibility; this.legacy=legacy;
    this.aiScores=aiScores;
  }

  private void role(Account user) {
    require(Set.of("PARENT","TUTOR").contains(user.role()),403,"Chỉ phụ huynh và gia sư sử dụng matching.");
  }
  private boolean ready(StudentProfile s) {
    return s.consentAt()!=null && !s.subjects().isEmpty() && !s.availability().isEmpty()
        && s.survey().keySet().containsAll(Set.of("goal","learning","difficulty","pace","support","lesson"))
        && s.survey().values().stream().allMatch(LEARNING::containsKey);
  }
  private boolean pending(State p, boolean parent) {
    return p==null || (p.status().equals("OPEN") && (p.expiresAt()==null || p.expiresAt().isAfter(Instant.now()))
        && (parent ? p.parentChoice() : p.tutorChoice())==null);
  }
  private List<Pair> matches(List<State> pairs) {
    return pairs.stream().filter(p -> p.status().equals("MATCHED"))
        .sorted(Comparator.comparing(State::matchedAt).reversed())
        .map(p -> new Pair(p.studentId(),p.tutorId(),p.status(),p.matchedAt())).toList();
  }
  private Card card(StudentProfile s,TutorProfile t,State pair,boolean parent) {
    var ai=aiScores.get(s,t);
    boolean incoming=pair!=null && "LIKE".equals(parent ? pair.tutorChoice() : pair.parentChoice());
    var preferences=s.survey().entrySet().stream().sorted(Map.Entry.comparingByKey())
        .map(e -> LEARNING.get(e.getValue())).filter(Objects::nonNull).distinct().toList();
    var preview = parent ? new TutorPreview(t.displayName(),t.photoUrl(),t.subjects(),t.grades(),t.modes(),t.province(),
        t.hourlyRate(),t.experience(),t.availability(),"Đã xác minh danh tính") : null;
    var student = parent ? null : new StudentPreview(s.grade(),s.subjects(),s.mode(),s.province(),s.budget(),
        s.availability(),preferences,LEARNING.getOrDefault(s.survey().get("goal"),"Hỗ trợ học tập")
            + " · " + LEARNING.getOrDefault(s.survey().get("difficulty"),"Củng cố kiến thức"));
    return new Card(s.id(),t.id(),incoming ? "INTERESTED_IN_YOU" : "SUGGESTED",
        ai.map(PairAiRepository.Score::level).orElse(eligibility.score(s,t)>=20 ? "STRONG" : "GOOD"),
        List.of("Đáp ứng môn học, lớp và hình thức học","Lịch học giao nhau, học phí trong ngân sách"),
        List.of("Mức phù hợp chỉ mang tính tham khảo; cần trao đổi sau khi kết nối."),
        ai.isPresent() ? "AI so sánh các thuộc tính học tập đã ẩn danh; cặp đáp ứng điều kiện bắt buộc. Mức phù hợp chỉ mang tính tham khảo."
            : "Gợi ý theo điều kiện học tập và kinh nghiệm, không dựa vào ảnh hay thành tích tự khai.",
        preview,student,pair==null ? null : pair.expiresAt(),
        true,pair==null ? "SUGGESTED" : pair.status(),
        ai.map(a -> (int)Math.round(a.score()*1000)).orElse(eligibility.score(s,t)),
        pair==null ? null : pair.matchedAt()!=null ? pair.matchedAt() : pair.firstLikedAt());
  }
  /** "Đã OK & Match": newest activity first (Match time, else the time of the first OK). */
  private List<Card> newestFirst(List<Card> cards) {
    return cards.stream().sorted(Comparator.comparing(Card::activityAt,Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
        .thenComparing(Card::tutorId).thenComparing(Card::studentId)).toList();
  }
  /** Featured tutors first, then profiles that already said OK, then by score. */
  private List<Card> sort(List<Card> cards) {
    var featured = tutorRepository.featuredIds();
    return cards.stream().sorted(Comparator.comparing((Card c) -> !featured.contains(c.tutorId()))
        .thenComparing((Card c) -> !c.tag().equals("INTERESTED_IN_YOU"))
        .thenComparing(Comparator.comparingInt(Card::ruleScore).reversed())
        .thenComparing(Card::tutorId).thenComparing(Card::studentId)).limit(20).toList();
  }
  @Transactional
  public Feed parentFeed(String student,String user) {
    var s=students.owned(student,user);
    require(ready(s),400,"Hãy hoàn thành 6 câu khảo sát và đồng ý lưu hồ sơ trước khi tìm gia sư.");
    Instant now=Instant.now();
    matching.expire(true,s.id(),now);
    var pairs=matching.forProfile(true,s.id());
    var byTutor=new HashMap<String,State>();
    pairs.forEach(p -> byTutor.put(p.tutorId(),p));
    var cards=new ArrayList<Card>();
    for(var t:tutorRepository.candidates(s)) {
      var pair=byTutor.get(t.id());
      if(pending(pair,true) && eligibility.eligible(s,t)) cards.add(card(s,t,pair,true));
    }
    var saved=new ArrayList<Card>();
    var savedIds=pairs.stream().filter(p -> "LIKE".equals(p.parentChoice()) && active(p))
        .map(State::tutorId).toList();
    for(var t:tutorRepository.findPublicByIds(savedIds)) {
      if(eligibility.compatible(s,t) && (byTutor.get(t.id()).status().equals("MATCHED") || eligibility.eligible(s,t))) saved.add(card(s,t,byTutor.get(t.id()),true));
    }
    return new Feed(sort(cards),matches(pairs),newestFirst(saved),!s.visible());
  }
  @Transactional
  public Feed tutorFeed(String user) {
    var t=tutors.own(user);
    tutors.publicTutor(t.id());
    Instant now=Instant.now();
    matching.expire(false,t.id(),now);
    var pairs=matching.forProfile(false,t.id());
    var byStudent=new HashMap<String,State>();
    pairs.forEach(p -> byStudent.put(p.studentId(),p));
    var cards=new ArrayList<Card>();
    var saved=new ArrayList<Card>();
    for(var s:studentRepository.candidates(t)) {
      var pair=byStudent.get(s.id());
      if(ready(s) && pending(pair,false) && eligibility.eligible(s,t)) cards.add(card(s,t,pair,false));
      if(ready(s) && pair!=null && "LIKE".equals(pair.tutorChoice()) && active(pair)
          && eligibility.compatible(s,t) && (pair.status().equals("MATCHED") || eligibility.eligible(s,t))) saved.add(card(s,t,pair,false));
    }
    return new Feed(sort(cards),matches(pairs),newestFirst(saved),!t.acceptingStudents());
  }
  /** Tutor ids pinned to the top of every list. */
  public java.util.Set<String> featuredTutors() { return tutorRepository.featuredIds(); }
  private boolean active(State p) {
    return p.status().equals("MATCHED") || (p.status().equals("OPEN")
        && p.expiresAt()!=null && p.expiresAt().isAfter(Instant.now()));
  }
  @Transactional
  public Result act(Account user,Action action) {
    role(user);
    boolean parent=user.role().equals("PARENT");
    // Gia sư chỉ thao tác trên hồ sơ của chính mình: kiểm tra TRƯỚC khi khóa bất cứ hàng nào.
    var found = parent ? tutors.find(action.tutorId()) : tutors.own(user.id());
    require(parent || found.id().equals(action.tutorId()), 404, "Không tìm thấy cặp phù hợp.");
    require(tutorRepository.lockActiveOwner(found.userId()), 404, "Hồ sơ không còn hoạt động.");
    var t = tutors.lock(found.id());
    tutors.publicTutor(t.id());
    var s=parent ? students.owned(action.studentId(),user.id()) : studentRepository.findById(action.studentId())
        .orElseThrow(() -> new BusinessException(404,"Không tìm thấy cặp phù hợp."));
    require(studentRepository.lockActiveParent(s.parentId()),404,"Hồ sơ không còn hoạt động.");
    s=studentRepository.lockById(s.id()).orElseThrow(() -> new BusinessException(404,"Không tìm thấy cặp phù hợp."));
    require(ready(s) && eligibility.eligible(s,t),400,"Cặp không còn đáp ứng điều kiện bắt buộc.");
    Instant now=Instant.now();
    var pair=matching.lockPair(s.id(),t.id());
    String own=parent ? pair.parentChoice() : pair.tutorChoice();
    String other=parent ? pair.tutorChoice() : pair.parentChoice();
    // Network retries preserve the existing decision and do not duplicate a Match.
    if(own!=null && (pair.status().equals("OPEN") || pair.status().equals("MATCHED")
        || (pair.status().equals("SKIPPED") && own.equals("SKIP"))) && (pair.status().equals("MATCHED")
        || pair.expiresAt()==null || pair.expiresAt().isAfter(now))) {
      require(own.equals(action.interested() ? "LIKE" : "SKIP"),409,"Lựa chọn đã được lưu và không thể đổi trong cùng cặp.");
      return new Result(pair.status(),pair.status().equals("MATCHED"),"Lựa chọn đã được lưu.");
    }
    require(pair.status().equals("OPEN") && (pair.expiresAt()==null || pair.expiresAt().isAfter(now)),409,"Cặp đã đóng hoặc hết hạn.");
    boolean incoming="LIKE".equals(other);
    boolean matched=action.interested() && incoming;
    matching.choose(parent,pair,action.interested(),matched,now);
    if(parent) legacy.save(s.id(),t.id(),action.interested());
    if(matched) matching.notifyMatch(s.id(),t.id());
    if(matched) matching.openConfirmation(s.id(),t.id(),now);
    return new Result(!action.interested() ? "SKIPPED" : matched ? "MATCHED" : "OPEN",matched,
        matched ? "Hai bên đã cùng OK. Bạn có một Match mới!" : action.interested()
            ? "Đã gửi sự quan tâm. Chờ bên kia cùng OK để tạo Match." : "Đã lưu lựa chọn.");
  }
  public List<Notification> notifications(Account user) { role(user); return matching.notifications(user.id()); }
  public void readNotification(String id,Account user) {
    role(user); require(matching.readNotification(id,user.id())==1,404,"Không tìm thấy thông báo.");
  }
}
