package com.green.growgreen.todo;

import com.green.growgreen.todo.model.*;
import com.green.growgreen.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class TodoService {
    private final TodoMapper MAPPER;

    @Autowired
    public TodoService(TodoMapper mapper) {
        this.MAPPER = mapper;
    }

    public int postTodo(TodoInsDto dto) {
        TodoEntity entity = new TodoEntity();
        entity.setIplant(dto.getIplant());
        entity.setCtnt(dto.getCtnt());
        entity.setRepeatYn(dto.getRepeatYn());
        entity.setDeadlineDate(dto.getDeadlineDate());
        entity.setDeadlineTime(dto.getDeadlineTime());

        //Todo등록
        MAPPER.insTodo(entity);

        // repeatYn이 1이면 p_day테이블에 인서트
        if( dto.getRepeatYn() == 1 ) {
            TodoRepeatDayDto repeatDto = new TodoRepeatDayDto();

            repeatDto.setItodo(entity.getItodo());

            for(int i=0; i<dto.getRepeatDay().size(); i++) {
                int repeatDay = dto.getRepeatDay().get(i);
                repeatDto.setRepeatDay(repeatDay);
                MAPPER.insRepeatDay(repeatDto);
            }
            return entity.getItodo();
        }
        return entity.getItodo();
    }

    public List<TodoVo> getTodo() {
        return MAPPER.selTodo();
    }

    public List<TodoVo> getTodoByDay(String deadline) {
        TodoSelDto dto = new TodoSelDto();
        dto.setDeadlineDate(deadline);

        return MAPPER.selTodoByDay(dto);
    }

    public List<TodoVo> getTodoAll(int year, int mon) {
        YearMonth today = null;
        if(year == 0 || mon == 0) {
            today = YearMonth.now();
        } else {
            today = YearMonth.of(year, mon);
        }
        LocalDate todayStart = today.atDay(1);// 시작날짜
        LocalDate todayEnd = today.atEndOfMonth();// 마지막 날짜

        LocalDate calStart = todayStart.plusDays(-todayStart.getDayOfWeek().getValue());// 실제캘린더에 찍히는 첫주 날짜
        LocalDate calEnd = todayEnd.plusDays(6 - todayEnd.getDayOfWeek().getValue());// 실제캘린더에 찍히는 마지막 주

        TodoSelListDto dto = TodoSelListDto.builder()
                .sDate(calStart.toString())
                .eDate(calEnd.toString())
                .build();

        List<TodoVo> list = MAPPER.selTodoAll(dto);
        List<TodoVo> result = new LinkedList();
        result.addAll(list);

        //반복 정보 get
        List<TodoRepeatVo> repeatInfoList = MAPPER.selTodoRepeat(today.toString());
        List<TodoVo> repeatVoList = createRepeatTodo(today, repeatInfoList);
        result.addAll(repeatVoList);
        return result;
    }

    private List<TodoVo> createRepeatTodo(YearMonth today, List<TodoRepeatVo> repeatInfoList) {
        List<TodoVo> list = new LinkedList();

        int targetYear = today.getYear(); // 들어오는 값의 YEAR
        int targetMon = today.getMonthValue();// 들어오는 값의 MONTH

        //1일의 요일을 찾으시오
        LocalDate targetStart = today.atDay(1);
        int startWeekOfMon = targetStart.getDayOfWeek().getValue();
        log.info("startWeekOfMon : {}" , startWeekOfMon);

        //마지막 날짜구하시오
        LocalDate targetEnd = today.atEndOfMonth();
        int endDayOfMonth = targetEnd.getDayOfMonth();
        log.info("endDayOfMonth : {}" , endDayOfMonth);

        // 반복등록해놓은 todo의 시작
        for(TodoRepeatVo vo :  repeatInfoList) {
            LocalDate voDate = vo.getDeadlineDate();
            int voYear = voDate.getYear();
            int voMon = voDate.getMonthValue();
            int voDay = voDate.getDayOfMonth();

            log.info("voYear : {}", voYear);
            log.info("voMon : {}", voMon);
            log.info("voDay : {}", voDay);

            // 반복의 요일을 구해옴
            String repeatStr = vo.getRepeatDay();
            String[] repeatWeekArr = repeatStr.split(",");

            for(String repeat : repeatWeekArr) {
                //리터럴값을 비교할때는 앞에 리터럴값을 넣어주고 비교하는것이 좋다.
                int dayOfWeek = "6".equals(repeat) ? 0 : Integer.parseInt(repeat) + 1;
                int lowDay = 1;

                //반복할 todo의 해당 요일 첫 날을 구할꺼예요.
                if (dayOfWeek != startWeekOfMon) {
                    lowDay += 7 - (startWeekOfMon - dayOfWeek);
                }

                if(targetYear == voYear && targetMon == voMon) {
                    while(lowDay <= voDay) {
                        lowDay += 7;
                    }
                }

                while(lowDay <= endDayOfMonth) {
                    TodoVo todoVo = new TodoVo();
                    todoVo.setItodo(vo.getItodo());
                    todoVo.setCtnt(vo.getCtnt());
                    todoVo.setDeadlineDate(String.format("%02d-%02d", voMon, lowDay));
                    todoVo.setDeadlineTime(vo.getDeadlineTime().toString());
                    todoVo.setNickNm(vo.getNickNm());
                    todoVo.setNm(vo.getNm());
                    todoVo.setFinishYn(vo.getFinishYn());
                    list.add(todoVo);
                    lowDay += 7;
                }
            }
        }

        return list;
    }

    public TodoAllDto getTodoDetail(int itodo) {
        TodoDetailVo todo = MAPPER.selTodoDetail(itodo);
        List<String> repeatDay = MAPPER.selTodoRepeatDay(itodo);

        return TodoAllDto.builder().todo(todo).repeatDay(repeatDay).build();
    }

    public int putTodo(TodoUpdDto dto) {
        //repeatYn이 0이나 1 모두 p_todo테이블의 todo데이터를 수정
        MAPPER.updTodo(dto); // p_todo테이블에서 todo 수정

        //repeatYn = 0 인 경우
        if( dto.getRepeatYn() == 0 ) {
            TodoDelDto delDto = new TodoDelDto();
            delDto.setItodo(dto.getItodo()); // repeatYn=1에서 0으로 바뀐 경우에는 p_day에 있는 반복 데이터를 지워야하니깐 필요한 작업
            MAPPER.delRepeatDay(delDto.getItodo());
        }

        //repeatYn = 1 인 경우를 if문으로 먼저 확인
        if( dto.getRepeatYn() == 1 ) {
            TodoRepeatDayDto repeatDto = new TodoRepeatDayDto();

            repeatDto.setItodo(dto.getItodo());

            //p_day테이블에 있는 기존 반복 데이터 삭제
            TodoDelDto delDto = new TodoDelDto();
            delDto.setItodo(dto.getItodo());
            MAPPER.delRepeatDay(delDto.getItodo());

            // for문으로 선택한 반복요일 만큼 p_day테이블에 insert
            for(int i=0; i<dto.getRepeatDay().size(); i++) {
                int repeatDay = dto.getRepeatDay().get(i);
                repeatDto.setRepeatDay(repeatDay);
                MAPPER.insRepeatDay(repeatDto);
            }
        }
        return 1;
    }

    public int putTodoFinish(int itodo) {
        return MAPPER.updTodoFinish(itodo);
    }

    public int deleteTodo(int itodo) {
        //p_day테이블에서 삭제
        MAPPER.delRepeatDay(itodo);
        //p_todo테이블에서 삭제
        return MAPPER.delTodo(itodo);
    }

    @Scheduled(cron = "0 0 0 ? * *")
    public void insUpdRepeatDay (){
        log.info("오늘 자 반복 인서트 됨");
        List<TodoSelRepeatDayVo> list = MAPPER.selRepeatTodo();
        int day = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getRepeatDay() == FileUtils.getDate()){
                day = FileUtils.getDate();
            }
        }
        MAPPER.insUpdRepeatDay(day);
    }
}
