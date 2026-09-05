package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites a catalogue description so its numbers are the resolved targets, not the base ones.
 *
 * <p>The catalogue's French copy embeds the Normal-tier numbers in the order the conditions declare
 * them (occurrences before the per-match target). Each numeric token that equals the next expected
 * base number is replaced by its resolved counterpart; anything else is left untouched, so a copy
 * that does not follow the convention keeps its original text rather than being mangled.
 */
final class ChallengeDescriptionResolver {

    /**
     * A French number: grouped thousands ("12 000") or a plain integer with an optional decimal
     * part ("0,90").
     */
    private static final Pattern NUMBER = Pattern.compile("\\d{1,3}(?: \\d{3})+|\\d+(?:,\\d+)?");

    /**
     * A standalone "1" followed by one or two plural words: the count and the words to agree.
     */
    private static final Pattern SINGULAR_UNIT = Pattern.compile(
        "(?<!\\d)1 (\\p{L}+)s(?!\\p{L})(?: (\\p{L}+)s(?!\\p{L}))?"
    );

    /**
     * Digits per thousands group.
     */
    private static final int GROUP_SIZE = 3;

    private ChallengeDescriptionResolver() {
    }

    /**
     * Rewrites the description with the resolved numbers.
     *
     * @param description catalogue description written for the base definition
     * @param base        definition as written in the catalogue
     * @param resolved    definition after target resolution, same conditions in the same order
     * @return the description carrying the resolved numbers
     */
    static String resolve(String description, ChallengeDefinition base, ChallengeDefinition resolved) {
        Objects.requireNonNull(description, "Challenge description must not be null.");
        if (base.conditions().size() != resolved.conditions().size()) {
            return description;
        }
        Deque<Replacement> pending = replacements(base, resolved);
        if (pending.isEmpty()) {
            return description;
        }
        Matcher matcher = NUMBER.matcher(description);
        StringBuilder rewritten = new StringBuilder(description.length());
        while (matcher.find()) {
            String token = matcher.group();
            Replacement next = pending.peek();
            String replacement = token;
            if (next != null && next.base().compareTo(parse(token)) == 0) {
                pending.poll();
                replacement = format(next.resolved(), decimals(token));
            }
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rewritten);
        return singularizeUnits(rewritten.toString());
    }

    /**
     * Drops the plural of the one or two words following a count that resolved to 1.
     *
     * <p>"1 parties compétitives" reads "1 partie compétitive"; a word without a trailing "s"
     * stops the agreement, so "1 kills ou plus" only touches "kills".
     */
    private static String singularizeUnits(String description) {
        Matcher matcher = SINGULAR_UNIT.matcher(description);
        StringBuilder agreed = new StringBuilder(description.length());
        while (matcher.find()) {
            String second = matcher.group(2) == null ? "" : " " + matcher.group(2);
            matcher.appendReplacement(agreed, Matcher.quoteReplacement("1 " + matcher.group(1) + second));
        }
        matcher.appendTail(agreed);
        return agreed.toString();
    }

    private static Deque<Replacement> replacements(ChallengeDefinition base, ChallengeDefinition resolved) {
        Deque<Replacement> pending = new ArrayDeque<>();
        for (int index = 0; index < base.conditions().size(); index++) {
            ChallengeCondition from = base.conditions().get(index);
            ChallengeCondition to = resolved.conditions().get(index);
            for (Replacement replacement : replacements(from, to)) {
                pending.add(replacement);
            }
        }
        return pending;
    }

    private static List<Replacement> replacements(ChallengeCondition from, ChallengeCondition to) {
        List<Replacement> ordered = new ArrayList<>();
        boolean perMatch = from.scope() == ChallengeScope.PER_MATCH;
        if (perMatch) {
            addCount(ordered, from.occurrences(), to.occurrences());
        }
        if (from.target() != null && to.target() != null) {
            ordered.add(new Replacement(from.target(), to.target()));
        }
        if (!perMatch) {
            addCount(ordered, from.occurrences(), to.occurrences());
        }
        addCount(ordered, from.minimumMatches(), to.minimumMatches());
        return ordered;
    }

    private static void addCount(List<Replacement> ordered, Integer from, Integer to) {
        if (from != null && to != null) {
            ordered.add(new Replacement(BigDecimal.valueOf(from), BigDecimal.valueOf(to)));
        }
    }

    private static BigDecimal parse(String token) {
        return new BigDecimal(token.replace(" ", "").replace(',', '.'));
    }

    private static int decimals(String token) {
        int comma = token.indexOf(',');
        return comma < 0 ? 0 : token.length() - comma - 1;
    }

    private static String format(BigDecimal value, int decimals) {
        BigDecimal scaled = value.setScale(decimals, RoundingMode.HALF_UP);
        String plain = scaled.abs().toPlainString().replace('.', ',');
        int comma = plain.indexOf(',');
        String integerPart = comma < 0 ? plain : plain.substring(0, comma);
        String fraction = comma < 0 ? "" : plain.substring(comma);
        StringBuilder grouped = new StringBuilder();
        int leading = integerPart.length() % GROUP_SIZE;
        if (leading > 0) {
            grouped.append(integerPart, 0, leading);
        }
        for (int start = leading; start < integerPart.length(); start += GROUP_SIZE) {
            if (!grouped.isEmpty()) {
                grouped.append(' ');
            }
            grouped.append(integerPart, start, start + GROUP_SIZE);
        }
        return (scaled.signum() < 0 ? "-" : "") + grouped + fraction;
    }

    /**
     * One number of the catalogue copy and the value it must now read.
     *
     * @param base     number written in the catalogue
     * @param resolved number after resolution
     */
    private record Replacement(BigDecimal base, BigDecimal resolved) {
    }
}
