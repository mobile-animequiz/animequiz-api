package com.animequiz.api.service;

import com.animequiz.api.service.LevelCalculator.LevelProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fórmula aprovada: xpParaIniciar(L) = 50 * L * (L - 1), nível máximo 50. */
class LevelCalculatorTest {

    private final LevelCalculator calculator = new LevelCalculator();

    @ParameterizedTest
    @CsvSource({"1,0", "2,100", "3,300", "4,600", "5,1000", "6,1500", "7,2100", "8,2800", "9,3600", "10,4500",
            "15,10500", "20,19000", "30,43500", "40,78000", "50,122500", "51,127500"})
    void xpToStartALevelFollowsTheApprovedFormula(int level, long expectedXp) {
        assertThat(calculator.xpToStartLevel(level)).isEqualTo(expectedXp);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "99,1", "100,2", "299,2", "300,3", "599,3", "600,4", "2450,7", "2799,7", "2800,8",
            "122499,49", "122500,50", "127499,50", "127500,50", "999999999,50"})
    void levelIsTheHighestOneWhoseStartDoesNotExceedTheXp(long xp, int expectedLevel) {
        assertThat(calculator.levelFor(xp)).isEqualTo(expectedLevel);
    }

    @Test
    void everyLevelStartsAndEndsConsistently() {
        for (int level = 1; level <= LevelCalculator.MAX_LEVEL; level++) {
            long start = calculator.xpToStartLevel(level);
            assertThat(calculator.levelFor(start)).as("início do nível %d", level).isEqualTo(level);
            if (level < LevelCalculator.MAX_LEVEL) {
                long lastXpOfLevel = calculator.xpToStartLevel(level + 1) - 1;
                assertThat(calculator.levelFor(lastXpOfLevel)).as("fim do nível %d", level).isEqualTo(level);
            }
        }
    }

    @Test
    void theWidthOfEachLevelIsOneHundredTimesTheLevel() {
        for (int level = 1; level <= LevelCalculator.MAX_LEVEL; level++) {
            LevelProgress progress = calculator.progressFor(calculator.xpToStartLevel(level));
            assertThat(progress.nextLevelXp() - progress.levelStartXp()).as("largura do nível %d", level)
                    .isEqualTo(100L * level);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,Iniciante", "4,Iniciante", "5,Aprendiz", "9,Aprendiz", "10,Explorador", "14,Explorador",
            "15,Veterano", "19,Veterano", "20,Mestre", "29,Mestre", "30,Grão-Mestre", "39,Grão-Mestre",
            "40,Lenda", "50,Lenda"})
    void ranksAreNeutralAndChangeAtTheApprovedLevels(int level, String expectedRank) {
        assertThat(calculator.rankTitleFor(level)).isEqualTo(expectedRank);
    }

    @Test
    void progressForZeroXpIsTheFirstLevel() {
        assertThat(calculator.progressFor(0)).isEqualTo(new LevelProgress(1, "Iniciante", 0, 100));
    }

    @Test
    void progressForTheSampleUserIsLevelSeven() {
        assertThat(calculator.progressFor(2450)).isEqualTo(new LevelProgress(7, "Aprendiz", 2100, 2800));
    }

    @Test
    void atTheMaximumLevelTheNextLevelXpStaysFiniteAndDifferentFromTheStart() {
        LevelProgress atCap = calculator.progressFor(122_500);
        LevelProgress beyondCap = calculator.progressFor(1_000_000);

        for (LevelProgress progress : new LevelProgress[] {atCap, beyondCap}) {
            assertThat(progress.level()).isEqualTo(50);
            assertThat(progress.rankTitle()).isEqualTo("Lenda");
            assertThat(progress.levelStartXp()).isEqualTo(122_500);
            assertThat(progress.nextLevelXp()).isEqualTo(127_500);
        }
    }

    @Test
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> calculator.levelFor(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.xpToStartLevel(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.xpToStartLevel(52)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.rankTitleFor(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.rankTitleFor(51)).isInstanceOf(IllegalArgumentException.class);
    }
}
