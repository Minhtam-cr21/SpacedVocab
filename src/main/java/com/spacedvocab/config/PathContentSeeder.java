package com.spacedvocab.config;

import com.spacedvocab.entity.Card;
import com.spacedvocab.entity.CardContent;
import com.spacedvocab.entity.CefrLevel;
import com.spacedvocab.entity.Deck;
import com.spacedvocab.entity.DeckVisibility;
import com.spacedvocab.entity.LearningPath;
import com.spacedvocab.entity.PathLesson;
import com.spacedvocab.entity.PathStage;
import com.spacedvocab.entity.User;
import com.spacedvocab.repository.CardRepository;
import com.spacedvocab.repository.DeckRepository;
import com.spacedvocab.repository.LearningPathRepository;
import com.spacedvocab.repository.PathLessonRepository;
import com.spacedvocab.repository.PathStageRepository;
import com.spacedvocab.repository.UserRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Loads the lessons of the CEFR path from {@code classpath:seed/cefr/<LEVEL>.txt} on startup (business-spec §6).
 * Each lesson becomes an OFFICIAL deck owned by the bootstrap admin ({@code ADMIN_EMAIL}), so nothing is
 * loaded without an admin. A lesson whose deck name already exists is skipped: running again is safe and
 * never overwrites what an admin edited.
 * <p>
 * File format: {@code @lesson Name | Description}, then one card per line
 * {@code word | ipa | partOfSpeech | meaning | example}; lines starting with {@code #} are comments.
 */
@Component
@Order(2) // after AdminAccountInitializer
public class PathContentSeeder implements ApplicationRunner {

    static final String PATH_NAME = "Lộ trình CEFR";
    private static final String LOCATION = "classpath:seed/cefr/*.txt";
    private static final Logger log = LoggerFactory.getLogger(PathContentSeeder.class);

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final LearningPathRepository pathRepository;
    private final PathStageRepository stageRepository;
    private final PathLessonRepository lessonRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final TransactionTemplate transaction;

    public PathContentSeeder(AdminProperties adminProperties, UserRepository userRepository,
                             LearningPathRepository pathRepository, PathStageRepository stageRepository,
                             PathLessonRepository lessonRepository, DeckRepository deckRepository,
                             CardRepository cardRepository, TransactionTemplate transaction) {
        this.adminProperties = adminProperties;
        this.userRepository = userRepository;
        this.pathRepository = pathRepository;
        this.stageRepository = stageRepository;
        this.lessonRepository = lessonRepository;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.transaction = transaction;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!adminProperties.isConfigured()) {
            log.info("ADMIN_EMAIL not set; skipping learning path content");
            return;
        }
        Optional<User> admin = userRepository.findByEmail(User.normalizeEmail(adminProperties.email()));
        Optional<Long> pathId = pathRepository.findByName(PATH_NAME).map(LearningPath::getId);
        if (admin.isEmpty() || pathId.isEmpty()) {
            log.warn("Admin account or path \"{}\" missing; skipping learning path content", PATH_NAME);
            return;
        }
        for (Resource file : new PathMatchingResourcePatternResolver().getResources(LOCATION)) {
            CefrLevel level = CefrLevel.valueOf(file.getFilename().replace(".txt", ""));
            List<SeedLesson> lessons = parse(file);
            int created = 0;
            for (int i = 0; i < lessons.size(); i++) {
                SeedLesson lesson = lessons.get(i);
                int position = i + 1;
                Boolean added = transaction.execute(status -> seed(admin.get(), pathId.get(), level, lesson, position));
                if (Boolean.TRUE.equals(added)) {
                    created++;
                }
            }
            log.info("Learning path {}: {} of {} lessons created", level, created, lessons.size());
        }
    }

    private boolean seed(User admin, Long pathId, CefrLevel level, SeedLesson lesson, int position) {
        if (deckRepository.findActiveIdByOwnerAndName(admin.getId(), lesson.name()).isPresent()) {
            return false;
        }
        PathStage stage = stageRepository.findByPathIdAndCefrLevel(pathId, level)
                .orElseThrow(() -> new IllegalStateException("stage " + level + " missing"));
        Deck deck = deckRepository.save(new Deck(admin, lesson.name(), lesson.description(), DeckVisibility.OFFICIAL));
        lesson.cards().forEach(content -> cardRepository.save(new Card(deck, content)));
        lessonRepository.save(new PathLesson(stage, deck, position));
        return true;
    }

    record SeedLesson(String name, String description, List<CardContent> cards) {
    }

    static List<SeedLesson> parse(Resource file) {
        List<SeedLesson> lessons = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("@lesson ")) {
                    String[] parts = line.substring("@lesson ".length()).split("\\|", 2);
                    lessons.add(new SeedLesson(parts[0].strip(), parts.length > 1 ? parts[1].strip() : null,
                            new ArrayList<>()));
                    continue;
                }
                String[] f = line.split("\\|", -1);
                if (f.length != 5 || lessons.isEmpty()) {
                    throw new IllegalStateException(file.getFilename() + ":" + number
                            + " expected 'word | ipa | partOfSpeech | meaning | example' after an @lesson line");
                }
                lessons.getLast().cards().add(new CardContent(f[0], f[3], f[1], f[2], f[4], null));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return lessons;
    }
}
