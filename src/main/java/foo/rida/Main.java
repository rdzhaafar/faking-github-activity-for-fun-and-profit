package foo.rida;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Path LAST_WRITE_FILE = Paths.get(".last_write");
    private static final Random RANDOM = new Random();

    private static final int COMMIT_ON_WEEKDAY_CHANCE = 87;
    private static final int COMMIT_ON_WEEKEND_CHANCE = 10;
    private static final int MIN_COMMITS_ON_WEEKDAY = 1;
    private static final int MAX_COMMITS_ON_WEEKDAY = 4;
    private static final int MIN_COMMITS_ON_WEEKEND = 1;
    private static final int MAX_COMMITS_ON_WEEKEND = 2;

    private record DateNumberOfCommitsPair(LocalDateTime date, int numberOfCommits) {
    }

    public static void main(String[] args) throws Exception {
        LocalDateTime lastWrite;
        try {
            lastWrite = parseLastWrite();
        } catch (IOException e) {
            LocalDateTime now = LocalDateTime.now();
            lastWrite = now.minusYears(1);
        }

        List<DateNumberOfCommitsPair> pairs = generateCommitsSince(lastWrite);
        for (DateNumberOfCommitsPair pair : pairs) {
            commit(pair);
        }
    }

    private static void commit(DateNumberOfCommitsPair generatedCommit) throws IOException, Exception {
        LocalDateTime date = generatedCommit.date();
        int numberOfCommits = generatedCommit.numberOfCommits();

        System.out.println("generated " + numberOfCommits + " commits for " + date);

        for (int i = 0; i < numberOfCommits; i++) {
            date = date.plusSeconds(1);
            Files.write(LAST_WRITE_FILE, date.toString().getBytes());

            execute("git", "add", LAST_WRITE_FILE.toString());
            execute("git", "commit", "-m", date.toString(), "--date=" + date);
        }
    }

    private static void execute(String ...command) throws Exception {
        final long timeoutSeconds = 2L;

        try {
            Process proc = new ProcessBuilder(command).start();
            if (!proc.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new Exception(
                        "command " + Arrays.toString(command) + " timed out after " + timeoutSeconds + " seconds");
            }
        } catch (InterruptedException e) {
            throw new Exception("command " + Arrays.toString(command) + " got interrupted", e);
        }
    }

    private static List<DateNumberOfCommitsPair> generateCommitsSince(LocalDateTime lastWrite) {
        List<DateNumberOfCommitsPair> pairs = new ArrayList<>();
        LocalDateTime iter = lastWrite;
        LocalDateTime now = LocalDateTime.now();
        while (iter.isBefore(now)) {
            int numberOfCommits = generateCommitsForDate(iter);
            DateNumberOfCommitsPair generated = new DateNumberOfCommitsPair(iter, numberOfCommits);
            pairs.add(generated);
            iter = iter.plusDays(1);
        }
        return pairs;
    }

    private static LocalDateTime parseLastWrite() throws IOException {
        String dateString = Files.readString(LAST_WRITE_FILE);
        return LocalDateTime.parse(dateString);
    }

    private static int generateCommitsForDate(LocalDateTime date) {
        DayOfWeek day = date.getDayOfWeek();
        boolean isWeekday = day.getValue() < 6;

        if (isWeekday) {
            boolean willCommitThisWeekday = rollChance(COMMIT_ON_WEEKDAY_CHANCE);
            if (!willCommitThisWeekday) {
                return 0;
            }
            return RANDOM.nextInt(MIN_COMMITS_ON_WEEKDAY, MAX_COMMITS_ON_WEEKDAY);
        } else {
            boolean willCommitThisWeekend = rollChance(COMMIT_ON_WEEKEND_CHANCE);
            if (!willCommitThisWeekend) {
                return 0;
            }
            return RANDOM.nextInt(MIN_COMMITS_ON_WEEKEND, MAX_COMMITS_ON_WEEKEND);
        }
    }

    private static boolean rollChance(int percent) {
        int roll = RANDOM.nextInt(101);
        return roll <= percent;
    }
}
