package com.example.demo.config;

import com.example.demo.Entity.QuestDataSource;
import com.example.demo.Entity.QuestTemplate;
import com.example.demo.Repository.QuestTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestDataInitializer implements CommandLineRunner {

    private final QuestTemplateRepository questTemplateRepository;

    @Override
    public void run(String... args) {
        if (questTemplateRepository.count() > 0) {
            return;
        }

        List<QuestTemplate> pool = List.of(
                QuestTemplate.builder().title("8,000보 걷기").description("오늘 하루 8,000보 이상 걷기").dataSource(QuestDataSource.STEPS).targetValue(8000).rewardMaterialName("이슬 한 방울").build(),
                QuestTemplate.builder().title("10,000보 걷기").description("오늘 하루 10,000보 이상 걷기").dataSource(QuestDataSource.STEPS).targetValue(10000).rewardMaterialName("활력초").build(),
                QuestTemplate.builder().title("7시간 이상 숙면").description("어젯밤 7시간 이상 수면하기").dataSource(QuestDataSource.SLEEP).targetValue(7).rewardMaterialName("홀쭉 열매").build(),
                QuestTemplate.builder().title("6시간 이상 숙면").description("어젯밤 6시간 이상 수면하기").dataSource(QuestDataSource.SLEEP).targetValue(6).rewardMaterialName("평온초").build(),
                QuestTemplate.builder().title("물 1.5L 마시기").description("오늘 하루 물 1.5L 이상 마시기").dataSource(QuestDataSource.MANUAL).rewardMaterialName("탱탱 젤리").build(),
                QuestTemplate.builder().title("카페인 컷오프 지키기").description("오후 2시 이후 카페인 섭취 금지").dataSource(QuestDataSource.MANUAL).rewardMaterialName("심해 오일").build(),
                QuestTemplate.builder().title("야식 먹지 않기").description("저녁 9시 이후 음식 섭취 금지").dataSource(QuestDataSource.MANUAL).rewardMaterialName("바나바잎").build(),
                QuestTemplate.builder().title("스트레칭 10분 하기").description("오늘 하루 10분 이상 스트레칭하기").dataSource(QuestDataSource.MANUAL).rewardMaterialName("황금 포자").build(),
                QuestTemplate.builder().title("아침 식사 챙기기").description("오늘 아침 식사를 챙겨 먹기").dataSource(QuestDataSource.MANUAL).rewardMaterialName("황금 레몬").build(),
                QuestTemplate.builder().title("취침 전 스마트폰 멀리하기").description("취침 30분 전부터 스마트폰 사용하지 않기").dataSource(QuestDataSource.MANUAL).rewardMaterialName("마룡 뿔").build()
        );

        questTemplateRepository.saveAll(pool);
    }
}
