import de.learnlib.algorithms.lstar.dfa.ClassicLStarDFA;
import de.learnlib.api.oracle.EquivalenceOracle.DFAEquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.oracle.equivalence.DFACompleteExplorationEQOracle;
import de.learnlib.oracle.equivalence.DFARandomWordsEQOracle;
import de.learnlib.oracle.equivalence.DFASimulatorEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import dk.brics.automaton.RegExp;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.brics.BricsDFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.*;


public class Main {

    static Alphabet<Character> printableAscii = Alphabets.characters('\u0020', '\u007A');
    static Alphabet<Character> unicode = Alphabets.characters('\u0000', '\uFFFE');

    static Appender csv = Appender.FileAppender("out.csv");

    public static void printDiagnostics() {
        long heapSize = Runtime.getRuntime().totalMemory();
        System.out.println("Heap Size = " + heapSize);
    }

    public static BricsDFA regexToDFA(String regex) {
        RegExp target = new RegExp(regex);
        BricsDFA dfa = new BricsDFA(target.toAutomaton(), true);

        return dfa;
    }

    public static DFAEquivalenceOracle<Character> perfectOracle(DFA<?, Character> dfa) {
        return new DFASimulatorEQOracle<>(dfa);
    }

    public static DFA<?, Character> learnRegex(String regex, Alphabet<Character> alphabet, int index) {
        // 1. Convert the regex into a LearnLib-compatible DFA
        BricsDFA dfa = regexToDFA(regex);

        // 2. Create the membership oracle
        MembershipOracle.DFAMembershipOracle<Character> memOracle = new SimulatorOracle.DFASimulatorOracle<>(dfa);

        // 3. Select the equivalence oracle strategy
//        DFARandomWordsEQOracle<Character> eqOracle = new DFARandomWordsEQOracle<>(memOracle, 0, 10, 100);
        DFAEquivalenceOracle<Character> eqOracle = perfectOracle(dfa);
        DFAEquivalenceOracle<Character> perfectEqOracle = perfectOracle(dfa);
//        DFACompleteExplorationEQOracle<Character> eqOracle = new DFACompleteExplorationEQOracle<>(memOracle, 1);

        // 4. Create the Learner and begin the learning cycle
        ClassicLStarDFA<Character> learner = new ClassicLStarDFA<>(alphabet, memOracle);

        DefaultQuery<Character, Boolean> counterexample = null;

        long start = System.currentTimeMillis();

        do {
            if (counterexample == null) {
                learner.startLearning();
            } else {
                System.out.println("refining with " + counterexample.toString());
                boolean refined = learner.refineHypothesis(counterexample);
                if (!refined) {
                    System.out.println("No refinement effected by counterexample!");
                }
            }

            counterexample = eqOracle.findCounterExample(learner.getHypothesisModel(), alphabet);

        } while (counterexample != null);

        long delta = System.currentTimeMillis() - start;

        // Append the learning result
        // Check if the hypothesis is equivalent
        DefaultQuery<?, ?> perfectCounterExample = perfectEqOracle.findCounterExample(learner.getHypothesisModel(), unicode);

        boolean equivalent = perfectCounterExample == null;
        int counterExampleLength = perfectCounterExample != null ? perfectCounterExample.getSuffix().length() : 0;

        csv.write(delta + "," + (equivalent ? "true" : "false") + "," + counterExampleLength + "," + regex);

        return learner.getHypothesisModel();
    }

    public static void main(String[] args) throws IOException {

        String benchmarks = "./regexlib-stratified-no-meta.re";
        Path path = Paths.get(benchmarks);
        BufferedReader reader = Files.newBufferedReader(path);

        int bad = 0, good = 0, timeout = 0;
        int n = 0;

        if (1 == 1) {
            printDiagnostics();
//            return;
        }

        // Select the alphabet
        Alphabet<Character> alphabet = unicode;

        // Create the executor for timeout purposes
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Learn each regex in sequence
        csv.line("# Alphabet = Unicode");
        csv.line("Index,Time(ms),Equivalent,CE-Length,Target");
        int i = 0;
        while (reader.ready()) {
            final int index = i;
            String targetRegex = reader.readLine().trim();

            System.out.println(n++ + " Creating Brics Automaton for " + targetRegex);

            csv.write(index + ",");

            // Learn the regex
            Future<DFA<?, ?>> handler = executor.submit((() -> {
                return learnRegex(targetRegex, alphabet, index);
            }));

            i++;

            try {
                handler.get(5, TimeUnit.MINUTES);
                good += 1;
            } catch (TimeoutException e) {
                handler.cancel(true);
                e.printStackTrace();
                timeout += 1;
            } catch (Exception e) {
                e.printStackTrace();
                bad += 1;
            }

            csv.write("," + targetRegex + "\n");
        }

        // Stop the executor
        executor.shutdownNow();

        // Report statistics and halt
        System.out.println(good);
        System.out.println(bad);
        System.out.println(timeout);
    }
}
