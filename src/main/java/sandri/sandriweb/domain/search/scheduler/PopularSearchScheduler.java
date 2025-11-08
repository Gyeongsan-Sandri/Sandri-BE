package sandri.sandriweb.domain.search.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.search.entity.PopularSearch;
import sandri.sandriweb.domain.search.repository.PopularSearchRepository;
import sandri.sandriweb.domain.search.repository.SearchLogRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularSearchScheduler {

    private final SearchLogRepository searchLogRepository;
    private final PopularSearchRepository popularSearchRepository;

    private static final int POPULAR_SEARCH_COUNT = 5;
    private static final int HOURS_24 = 24;

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void aggregatePopularSearches() {
        log.info("인기 검색어 집계 시작");

        try {
            LocalDateTime startTime = LocalDateTime.now().minusHours(HOURS_24);
            List<Object[]> searchCounts = searchLogRepository.countSearchesByKeyword(startTime);

            if (searchCounts.isEmpty()) {
                log.info("집계할 검색 로그가 없습니다.");
                return;
            }

            List<PopularSearch> previousSearches = popularSearchRepository.findPreviousPopularSearches();
            Map<String, Integer> previousRankMap = new HashMap<>();
            previousSearches.forEach(ps -> previousRankMap.put(ps.getKeyword(), ps.getRank()));

            popularSearchRepository.disableAllPopularSearches();

            LocalDateTime aggregatedAt = LocalDateTime.now();
            AtomicInteger rankCounter = new AtomicInteger(1);
            List<PopularSearch> newPopularSearches = searchCounts.stream()
                    .limit(POPULAR_SEARCH_COUNT)
                    .map(result -> {
                        String keyword = (String) result[0];
                        Long searchCount = ((Number) result[1]).longValue();
                        Integer previousRank = previousRankMap.get(keyword);
                        int currentRank = rankCounter.getAndIncrement();
                        
                        return PopularSearch.builder()
                                .rank(currentRank)
                                .keyword(keyword)
                                .searchCount(searchCount)
                                .previousRank(previousRank)
                                .aggregatedAt(aggregatedAt)
                                .build();
                    })
                    .collect(Collectors.toList());

            popularSearchRepository.saveAll(newPopularSearches);

            log.info("인기 검색어 집계 완료: {}개 키워드", newPopularSearches.size());
        } catch (Exception e) {
            log.error("인기 검색어 집계 중 오류 발생", e);
        }
    }
}

