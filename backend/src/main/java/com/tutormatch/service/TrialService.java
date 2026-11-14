package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;
import com.tutormatch.dto.lesson.TrialDtos.*;
import com.tutormatch.exception.BusinessException;
import com.tutormatch.model.Account;
import com.tutormatch.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrialService {
  private final ConnectionService connections;
  private final TrialRepository trials;
  private final MatchingRepository matching;
  private final org.springframework.transaction.support.TransactionTemplate transaction;
  public TrialService(ConnectionService connections,TrialRepository trials,MatchingRepository matching,
      org.springframework.transaction.PlatformTransactionManager manager) {
    this.connections=connections; this.trials=trials; this.matching=matching;
    this.transaction=new org.springframework.transaction.support.TransactionTemplate(manager);
  }
  private BusinessException missing() { return new BusinessException(404,"Không tìm thấy lịch học thử."); }
  private List<Instant> slots(List<Instant> values) {
    require(values!=null && !values.isEmpty() && values.size()<=3,400,"Đề xuất từ 1 đến 3 khung giờ.");
    Instant now=Instant.now();
    require(values.stream().allMatch(v -> v!=null && v.isAfter(now.plusSeconds(3600))
        && v.isBefore(now.plus(Duration.ofDays(90)))),400,"Chọn giờ sau ít nhất một giờ và trong 90 ngày.");
    require(new HashSet<>(values).size()==values.size(),400,"Các khung giờ không được trùng nhau.");
    return values.stream().sorted().toList();
  }
  private Instant deadline(List<Instant> slots) {
    return slots.getFirst().isBefore(Instant.now().plus(Duration.ofDays(3)))
        ? slots.getFirst() : Instant.now().plus(Duration.ofDays(3));
  }
  private void event(Trial t,String kind,String message) {
    matching.notifyEvent(t.studentId(),t.tutorId(),"TRIAL_"+t.id()+"_"+t.version()+"_"+kind,message,null);
  }
  private void expire(Trial t) {
    if (t.status().equals("PENDING") && !t.expiresAt().isAfter(Instant.now())) {
      trials.close(t.id(),"EXPIRED",null);
      event(t,"EXPIRED","Đề xuất học thử đã hết hạn phản hồi. Bạn có thể đề xuất lịch mới.");
    }
  }
  @Transactional(noRollbackFor=BusinessException.class)
  public Trial propose(Account user,Propose r) {
    var values=slots(r.slots());
    var context=connections.requireConnected(user,r.studentId(),r.tutorId());
    require(r.mode()==null || r.mode().equals(context.s().mode()),400,"Hình thức học phải đúng hồ sơ học sinh đã kết nối.");
    require(!trials.legacyActive(r.studentId(),r.tutorId()),409,"Cặp còn lịch học thử từ luồng cũ đang hoạt động.");
    for (var t : trials.pair(r.studentId(),r.tutorId())) expire(t);
    require(trials.pair(r.studentId(),r.tutorId()).stream().noneMatch(t -> Set.of("PENDING","CONFIRMED").contains(t.status())),
        409,"Cặp đã có lịch đang hoạt động. Hãy phản hồi hoặc hủy lịch đó trước.");
    String id=UUID.randomUUID().toString();
    trials.create(id,r.studentId(),r.tutorId(),user.role(),values,deadline(values),context.s().mode());
    var trial=trials.find(id).orElseThrow(this::missing);
    event(trial,"PROPOSED","Có đề xuất giờ học thử mới. Mở lịch học thử để phản hồi.");
    return trial;
  }
  @Transactional(noRollbackFor=BusinessException.class)
  public Trial respond(Account user,String id,Respond r) {
    var found=trials.find(id).orElseThrow(this::missing);
    connections.requireConnected(user,found.studentId(),found.tutorId());
    var t=trials.lock(id).orElseThrow(this::missing);
    expire(t);
    t=trials.lock(id).orElseThrow(this::missing);
    require(t.version()==r.version(),409,"Lịch đã thay đổi. Hãy cập nhật trạng thái rồi thử lại.");
    require(Set.of("PENDING","CONFIRMED").contains(t.status()),409,"Lịch đã hủy hoặc hết hạn.");
    if (r.action().equals("CANCEL")) {
      require(t.chosenAt()==null || t.chosenAt().isAfter(Instant.now()),409,"Chỉ hủy lịch trước giờ học đã chốt.");
      require(r.reason()!=null && !r.reason().isBlank(),400,"Vui lòng nhập lý do hủy lịch.");
      require(r.reason().length()<=500,400,"Lý do tối đa 500 ký tự.");
      trials.close(id,"CANCELLED",r.reason().trim());
      event(t,"CANCELLED","Lịch học thử đã hủy. Mở lịch để xem lý do.");
    } else {
      require(t.status().equals("PENDING"),409,"Lịch đã chốt, không thể phản hồi lần hai.");
      require(!t.proposerRole().equals(user.role()),409,"Đang chờ bên kia phản hồi đề xuất của bạn.");
      if (r.action().equals("ACCEPT")) {
        require(r.slot()!=null && t.slots().contains(r.slot()) && r.slot().isAfter(Instant.now()),400,"Chọn một giờ trong đề xuất còn hiệu lực.");
        trials.accept(id,r.slot());
        event(t,"CONFIRMED","Hai bên đã chốt lịch học thử. Mở lịch để xem giờ đã xác nhận.");
      } else {
        require(r.action().equals("COUNTER"),400,"Thao tác không hợp lệ.");
        var values=slots(r.slots());
        trials.counter(id,user.role(),values,deadline(values));
        event(t,"COUNTER","Có đề xuất giờ học thử khác. Mở lịch để phản hồi.");
      }
    }
    return trials.lock(id).orElseThrow(this::missing);
  }
  @Transactional(noRollbackFor=BusinessException.class)
  public List<Trial> pair(Account user,String student,String tutor) {
    connections.requireConnected(user,student,tutor);
    for (var t : trials.pair(student,tutor)) expire(t);
    return trials.pair(student,tutor);
  }
  public List<Trial> list(Account user) {
    require(Set.of("PARENT","TUTOR").contains(user.role()),403,"Chỉ phụ huynh và gia sư có lịch học thử.");
    for (var t : trials.list(user)) {
      // Commit each pair separately rather than accumulating profile locks across a whole inbox.
      transaction.executeWithoutResult(ignored -> {
        connections.open(user,t.studentId(),t.tutorId(),true);
        expire(trials.lock(t.id()).orElseThrow(this::missing));
      });
    }
    return trials.list(user);
  }
}
