import org.junit.jupiter.api.*;

import com.benny.wordle.RegExWordle;
import com.benny.wordle.RegExWordle.LetterResponse;
import static com.benny.wordle.RegExWordle.LetterResponse.*;
import com.benny.wordle.RegExWordle.WordleResponse;
import com.benny.wordle.RegExWordle.Guess;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RegexWordleTest {


    /** Helper Method so we dont have to manually write each tests 5 WordleResponses
     also made to help deal with a letter that appears multiple times in a word
     ex:
                        G
     secret = ['a','p','p','l','e']
     guess  = ['p','a','p','e','r']

     first loop:
     i=0:  guess p ≠ secret a → remainingChars adds 'a'
     i=1:  guess a ≠ secret p → remainingChars adds 'p'
     i=2:  guess p == secret p → colors[2] = GREEN
     i=3:  guess e ≠ secret l → remainingChars adds 'l'
     i=4:  guess r ≠ secret e → remainingChars adds 'e'
     colors = [  ,   , G,   ,   ]
     remainingChars = [a, p, l, e]

     second loop:
     i=0: colors[0]==null, guessChars[0]='p'.
     remainingChars.remove('p') returns true → colors[0]=YELLOW,
     remainingChars is now [a, l, e].

     i=1: colors[1]==null, guessChars[1]='a'.
     remove('a') → true → colors[1]=YELLOW,
     remainingChars now [l, e].

     i=2: skip (already GREEN).

     i=3: colors[3]==null, guessChars[3]='e'.
     remove('e') → true → colors[3]=YELLOW,
     remainingChars now [l].

     i=4: colors[4]==null, guessChars[4]='r'.
     remove('r') → false → colors[4]=GRAY.

     then we return colors = [Y, Y, G, Y, X]
     */
    private List<WordleResponse> feedback(String secret, String guess) {

        char[] secretChars = secret.toLowerCase().toCharArray(); // ['a','p','p','l','e']
        char[] guessChars = guess.toLowerCase().toCharArray();  // ['p','a','p','e','r']

        LetterResponse[] colors = new LetterResponse[secretChars.length];
        List<Character> remainingChars = new ArrayList<>();

        // First pass: mark any greens and add any unmarked letter into remainingChars[]
        for (int i = 0; i < secretChars.length; i++) {
            if (guessChars[i] == secretChars[i]) {
                colors[i] = CORRECT_LOCATION;
            } else {
                remainingChars.add(secretChars[i]);
            }
        }
        // Second pass: for yellows/grays, see if we can pull a yellow out of remainingChars
        for (int i = 0; i < guessChars.length; i++) {
            if (colors[i] != null) continue; //skip over any index already marked with response aka greens
            if (remainingChars.remove((Character)guessChars[i])) {// remove() returns true if that letter was in the list
                colors[i] = WRONG_LOCATION; //mark yellow since
            } else {
                colors[i] = WRONG_LETTER; //mark gray
            }
        }

        // encapsulate all 5 WordleResponse objects to be returned
        List<WordleResponse> result = new ArrayList<>(secretChars.length);
        for (int i = 0; i < guessChars.length; i++) {
            result.add(new WordleResponse(guessChars[i], i, colors[i]));
        }
        return result;
    }


    //private List<Guess> history;

    @BeforeEach
    void clearGrayState() {
        resetGrayCharacterClass();
        //history = new ArrayList<>();
    }


    @Test
    void testGreenLookaheads(){
        String greenRegexSegment = CORRECT_LOCATION.regexSegment(2, 'c');
        assertEquals("(?=.{" + 2 + "}c)", greenRegexSegment);
    }

    @Test
    void testYellowLookaheads(){
        String yellowRegexSegment = WRONG_LOCATION.regexSegment(1, 'x');
        String expected = "(?=.*x)" + "(?!.*^.{" + 1 + "}x)"; //yellows contain two concatenated lookaheads
        assertEquals(expected,yellowRegexSegment);
    }

    @Test
    void testGrayLookaheads(){
        String secret = "xxxxx";
        List<WordleResponse> response1 = feedback(secret, "ababa");
        Guess guess1 = new Guess("aaaaa", response1);
        List<Guess> history = List.of(guess1);
        Pattern actual1 = RegExWordle.getUpdatedRegexRule(history);
        //without the anchors we see (?!.*[ab]) which is a single charcter class
        String expectedCharacterClass1 = "(?i)^(?!.*[ab]).{5}$" ;
        assertEquals(expectedCharacterClass1, actual1.toString());


        List<WordleResponse> response2 = feedback(secret, "babba");
        Guess guess2 = new Guess("aaaaa", response2);
        history = List.of(guess2);
        Pattern actual2 = RegExWordle.getUpdatedRegexRule(history);
        //we made a new guess but still the character class and remaining pattern should be unchanged
        String expectedCharacterClass2 = "(?i)^(?!.*[ab]).{5}$" ;
        assertEquals(expectedCharacterClass2, actual2.toString());


        List<WordleResponse> response3 = feedback(secret, "cable");
        Guess guess3 = new Guess("aaaaa", response3);
        history = List.of(guess3);
        Pattern actual3 = RegExWordle.getUpdatedRegexRule(history);
        //now we guess new gray letters on a new turn and still we only have 1 character class added to the pattern
        String expectedCharacterClass3 = "(?i)^(?!.*[abcel]).{5}$" ;
        assertEquals(expectedCharacterClass3, actual3.toString());
    }




    /** Finally testing our most important wordleMatches method
     * Shows us that guessing “TRAIN” results in all grays against secret “SHLEP”
     * asserts that we are handling the filtering of possible answer candidates correctly */
    @Test
    void wordleMatches_allGrayExcludesThoseLetters() {
        List<WordleResponse> feedbackColors = feedback("shlep", "train");
        Guess guessEntry = new Guess("train", feedbackColors);
        List<Guess> history = List.of(guessEntry);
        List<String> filteredCandidatesDictionary = RegExWordle.wordleMatches(history);

        // never re‑suggest “train”
        assertFalse(filteredCandidatesDictionary.contains("train"));

        // none of the letters T,R,A,I,N appear
        assertFalse(filteredCandidatesDictionary.stream().anyMatch(w -> w.matches(".*[train].*")));

        // ex: “queue” (no T/R/A/I/N) should still be present
        assertTrue(filteredCandidatesDictionary.contains("queue"));
    }

    @Test
    void wordleMatches_combinedYellowAndGreenAndGray() {
        String secret = "shlep";
        // Turn 1: TRAIN → all gray
        Guess guess1 = new Guess("train", feedback(secret, "train"));
        // Turn 2: COUGH → only H is yellow, while C,O,U,G are gray
        Guess guess2 = new Guess("cough", feedback(secret, "cough"));
        List<Guess> history = new ArrayList<>(List.of(guess1, guess2));
        List<String> filteredCandidatesDictionary = RegExWordle.wordleMatches(history);

        assertFalse(filteredCandidatesDictionary.contains("train"));
        assertFalse(filteredCandidatesDictionary.contains("cough"));


        String expectedCombinedRegex = "(?i)^(?=.*h)(?!.*^.{4}h)(?!.*[arctugino]).{5}$";
        Pattern patternUsed = RegExWordle.getUpdatedRegexRule(history);
        assertEquals(expectedCombinedRegex,patternUsed.toString()); //even though these are identical at surface level, under the hood Java’s Pattern class doesn’t override equals() to compare the compiled pattern text, so we compare as strings


        //asserts that...? finish this note
        assertTrue(filteredCandidatesDictionary.stream().allMatch(patternUsed.asPredicate()));

        //Turn 3: SHIPS → S and H are greens and P is yellow, second S is grey since there is only 1 in word that was already matched
        Guess guess3 = new Guess("ships", feedback(secret, "ships"));
        history.add(guess3);
        filteredCandidatesDictionary = RegExWordle.wordleMatches(history); //update dictionary for new round
        Pattern actual = RegExWordle.getUpdatedRegexRule(history);
        String expected = "(?i)^(?=.*h)(?!.*^.{4}h)(?=.{0}s)(?=.{1}h)(?=.*p)(?!.*^.{3}p)(?!.*[arcstugino]).{5}$";
        assertEquals(expected,actual.toString());

    }


    @Test
    void testAllGreenClearsGray() {
        String secret = "apple";
        List<WordleResponse> allGreenFeedback = feedback(secret,"apple");
        Guess winningGuess = new Guess("apple", allGreenFeedback);
        List<Guess> history = List.of(winningGuess);
        // should get exactly five green lookaheads, no gray class or yellows
        Pattern rule = RegExWordle.getUpdatedRegexRule(history);
        String expected =
                "(?i)^" +
                        "(?=.{" + 0 + "}a)" +
                        "(?=.{" + 1 + "}p)" +
                        "(?=.{" + 2 + "}p)" +
                        "(?=.{" + 3 + "}l)" +
                        "(?=.{" + 4 + "}e)" +
                        ".{5}$";
        assertEquals(expected, rule.toString());
    }


    @Test
    void testDuplicateLettersFeedback() {
        String secret = "allee";
        List<WordleResponse> duplicateFeedback = feedback(secret, "eagle");
        Guess guess = new Guess("eagle", duplicateFeedback); // expected: E→Y, A→Y, G→X, L→Y, E→G
        List<Guess> history = List.of(guess);
        String actual = RegExWordle.getUpdatedRegexRule(history).toString();
        String expected = "(?i)^(?=.*e)(?!.*^.{0}e)(?=.*a)(?!.*^.{1}a)(?=.*l)(?!.*^.{3}l)(?=.{4}e)(?!.*[g]).{5}$";
        assertEquals(expected, actual);
    }


    @Test
    void testDuplicateLettersFeedback_ConsolePrint() {
        String secret    = "allee";
        String guessWord = "eagle";

        List<WordleResponse> duplicateFeedback = feedback(secret, guessWord);

        // print a header with secret vs. guess
        System.out.println("===== testDuplicateLettersFeedback_ConsolePrint =====");
        System.out.println("Secret: " + secret.toUpperCase());
        System.out.println("Guess : " + guessWord.toUpperCase());
        System.out.println("Results:");

        //one line per position
        for (WordleResponse response : duplicateFeedback) {
            int position = duplicateFeedback.indexOf(response);
            char letter = Character.toUpperCase(response.getLetter());
            String resp  = response.getResponse().name();
            System.out.printf("  position %d: %c → %s\n", position, letter, resp);
        }

        //assert that you got exactly the 5 responses you'd expect:
        List<LetterResponse> want = List.of(
                WRONG_LOCATION,
                WRONG_LOCATION,
                WRONG_LETTER,
                WRONG_LOCATION,
                CORRECT_LOCATION
        );
        List<LetterResponse> got = new ArrayList<>();
        for (WordleResponse r : duplicateFeedback) {
            got.add(r.getResponse());
        }
        assertEquals(want, got);
    }

}
