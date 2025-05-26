package com.benny.wordle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegExWordle {

    private RegExWordle() {
        throw new AssertionError("RegExWordle is static-only; do not instantiate");
    }


    /**
     * Our smallest unit of a Wordle Guess feedback
     * we have an enum for all 3 outcomes of a letter guessed
     * each response has its own regex rule that it returns to be appended when we call it's function
     * these rules will be combined and rewrote each turn in to make a new dictionary filter for wordleMatches() in its helper method getUpdatedRegexRule()
     */
    public enum LetterResponse {
        CORRECT_LOCATION {
            @Override   //Green
            public String regexSegment(int position, char letter) {
                if(GrayCharacterClass.grayLetters.contains(letter)) GrayCharacterClass.remove(letter);
                return "(?=.{" + position + "}" + letter + ")"; //positive lookahead to for this letter in this exact position for all future guesses
            }
        },
        WRONG_LOCATION {
            @Override   //Yellow
            public String regexSegment(int position, char letter) {
                if(GrayCharacterClass.grayLetters.contains(letter)) GrayCharacterClass.remove(letter);
                return "(?=.*" + letter + ")" //1) positive lookahead for this yellow letter anywhere
                        + "(?!.*^.{" + position + "}" + letter + ")"; //2) negative lookahead for this yellow letter in that same position for future
            }
        },
        WRONG_LETTER {
            @Override // gray
            public String regexSegment(int position, char letter) { //accumulate all gray letters then return the entire character‑class in one look‑ahead
                GrayCharacterClass.add(letter);
                //return GrayCharacterClass.makeRule(); //we only want one encompassing character class and this will redundantly return one for each letter so instead it returns nothing while adding to the CC that we build in getUpdatedRegexRule()
                return "";
            }
        };

       //abstract class for all enums to incorporate
        public abstract String regexSegment(int position, char letter);

        /**
         * helper to collect all gray letters and build one character class (?!.*[...]) block
         */
        private static class GrayCharacterClass { //TODO note read below
            private static final Set<Character> grayLetters = new HashSet<>();


            static void add(char c) {
                grayLetters.add(Character.toLowerCase(c));
            }

            static void remove(char c) {
                grayLetters.remove(Character.toLowerCase(c));
            }

            static String makeRule() {
                if (grayLetters.isEmpty()) return "";
                StringBuilder sb = new StringBuilder("(?!.*[");
                for (char c : grayLetters) sb.append(c);
                sb.append("])");
                return sb.toString();
            }

            static void clear() {
                grayLetters.clear();
            }
        }

        //called in tests,needed public accesor for private method
        public static void resetGrayCharacterClass() {
            GrayCharacterClass.clear();
        }
    }

    //static field belonging to class, compiles at runtime? is that right? is that even a chiddush?
    //private static final Set<Character> grayLetters = new HashSet<>(); //consider making this synchronized if possible


    /**
     * Each time the player inputs a legal guess each of the guesses 5 letters gets it's own response container object
     */
    public static class WordleResponse {
        char letter;
        int index;
        LetterResponse response;

        public WordleResponse(char letter, int index, LetterResponse response) {
            this.letter = letter;
            this.index = index;
            this.response = response;
        }

        //added the below for testing
        public char getLetter() {
            return letter;
        }
        public LetterResponse getResponse() {
            return response;
        }
    }

    /**
     * In order for our game to be able to remember the player's guesses we need all of it's data in an object container
     * Each Guess holds the data of previous round's word guessed and its corresponding 5 LetterResponses feedback held in 1 WordleResponse object
     */
    public static class Guess {
        String wordGuessed;
        List<WordleResponse> feedback;

        public Guess(String wordGuessed, List<WordleResponse> feedback) {
            this.wordGuessed = wordGuessed;
            this.feedback = feedback;
        }
    }



    //List of 14,855 possible words eligible for a legal guess (consider lazy initialization in try catch block later when finalizing after we have working product)
    //would't it make sense for these to all be sets since it's supposed to be individual words? or is it more efficient to use a list and we can assume my source will not include doubles anyways?
    private static final List<String> dictionaryOfAllLegalGuesses = loadWordList("src/all_possible_wordles.txt", String::toLowerCase);

    //could be useful for randomly choosing legal words as a secret target to guess
    private static final List<String> everyPreviousWinningWordle = loadWordList("wordle_history.csv", line -> line.split(",", 2)[1].toLowerCase()); //TODO i should consider how this would be done with streams theoretically as study material

    /**
     * The CSV of past Wordles scraped from NYT contains each word's date
     * while that may be useful for future wordle projects we will strip away the dates for now to hold just words.
     *
     * update: this method is now a general loader method that uses java 8 streams as study material for the final
     * it will read the supplied file and operate with the given mapping parameter
     * @param mapper in our case will either map every word to become lowercased, or strip the date that a word was officially used by NYT but also lowercase it, ensuring canonical form
     */

    private static List<String> loadWordList(String path, Function<String, String> mapper) {
        try {
            return Files.readAllLines(Paths.get(path))
                    .stream()
                    .map(mapper)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load word list from " + path, e);
        }
    }


    /** Main method of the HW for Q #5
     * we start by loading the users unique guess history into a HashSet called wordsTried under our lowercase canonical form
     * then we call our helper method that builds the most up to date Regex Pattern used to filter the dictionary based on our history
     * we then filter the dictionary by using our new Regex rule and leaving out all wordsTried and return the now smaller dictionary as a List of Strings
     * @param history is a list of Guesses which are themselves each their own lists of WordleResponses
     * @return List of all words not ruled out by guess history filtering AKA returns collection of all currently valid guesses
     */
    public static List<String> wordleMatches(List<Guess> history) {
        // 1) remember which words we've already tried
        Set<String> wordsTried = new HashSet<>();
        for (Guess g : history) {
            wordsTried.add(g.wordGuessed.toLowerCase());
        }

        //2) i made a helper method to deal with regex building plus it makes them accessible for testing and therefore SRP compliant
        Pattern newRegexRule = getUpdatedRegexRule(history);

        // 3) filter the dictionary
        List<String> filteredDictionary = new ArrayList<>();
        for (String candidate : dictionaryOfAllLegalGuesses) {
            if (wordsTried.contains(candidate.toLowerCase())) continue;
            if (newRegexRule.matcher(candidate).matches()) {
                filteredDictionary.add(candidate);
            }
        }
        return filteredDictionary;
    }

    /** build the big regex by replaying every response */
    public static Pattern getUpdatedRegexRule(List<Guess> history){
        LetterResponse.GrayCharacterClass.clear(); // is it bad or even worse to use LetterResponse.GrayCharacterClass.grayLetters.clear();
        StringBuilder patternBuilder = new StringBuilder("(?i)^"); //start with ^ to anchor us to start search at begining of the resulting string and (?i) as case insensitive flag
        for (Guess guessentry : history) {
            for (WordleResponse letter : guessentry.feedback) {
                //we build the new regex rule each turn by appending the responses from every previous guess onto eachother for one massive filter
                patternBuilder.append(letter.response.regexSegment(letter.index, letter.letter));
            }
        }
        patternBuilder.append(LetterResponse.GrayCharacterClass.makeRule());
        patternBuilder.append(".{5}$");// ending with this always enforces we accept exactly 5 letters
        return Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }


//TODO below is a simple main below was made by chatGPT as a View/Controller to interact with my above model before I wrote any tests (i figured if it wasn't a HW requirement there wouldn't be a problem delegating that work)

//    public static void main(String[] args) throws IOException {
//        // ANSI background colors
//        final String BG_GREEN  = "\u001B[42m";
//        final String BG_YELLOW = "\u001B[43m";
//        final String BG_GRAY   = "\u001B[100m";
//        final String RESET     = "\u001B[0m";
//
//        Scanner in = new Scanner(System.in);
//        String secret = everyPreviousWinningWordle
//                .get(new Random().nextInt(everyPreviousWinningWordle.size()));
//
//        System.out.println("🕹️  Welcome to RegExWordle!");
//        System.out.println("Guess the 5‑letter word in 6 tries.\n");
//
//        List<Guess> history = new ArrayList<>();
//
//        GAME: for (int turn = 1; turn <= 6; turn++) {
//            // read a valid guess
//            String guess;
//            while (true) {
//                System.out.print("Turn " + turn + " — your guess: ");
//                guess = in.nextLine().trim().toLowerCase();
//                if (guess.length() != 5) {
//                    System.out.println("   ↳ must be exactly 5 letters");
//                } else if (!dictionaryOfAllLegalGuesses.contains(guess)) {
//                    System.out.println("   ↳ word not in dictionary");
//                } else {
//                    break;
//                }
//            }
//
//            // build feedback: greens first, then yellows vs grays
//            List<WordleResponse> fb = new ArrayList<>(5);
//            boolean[] used = new boolean[5];
//            // greens
//            for (int i = 0; i < 5; i++) {
//                if (guess.charAt(i) == secret.charAt(i)) {
//                    fb.add(new WordleResponse(guess.charAt(i), i, LetterResponse.CORRECT_LOCATION));
//                    used[i] = true;
//                } else {
//                    fb.add(null);
//                }
//            }
//            // count leftover secret letters
//            Map<Character,Integer> count = new HashMap<>();
//            for (int i = 0; i < 5; i++) {
//                if (!used[i]) {
//                    count.merge(secret.charAt(i), 1, Integer::sum);
//                }
//            }
//            // yellows & grays
//            for (int i = 0; i < 5; i++) {
//                if (fb.get(i) != null) continue;
//                char c = guess.charAt(i);
//                if (count.getOrDefault(c,0) > 0) {
//                    fb.set(i, new WordleResponse(c, i, LetterResponse.WRONG_LOCATION));
//                    count.put(c, count.get(c)-1);
//                } else {
//                    fb.set(i, new WordleResponse(c, i, LetterResponse.WRONG_LETTER));
//                }
//            }
//
//            history.add(new Guess(guess, fb));
//
//            // display with colored backgrounds
//            System.out.print("   Feedback: ");
//            for (WordleResponse r : fb) {
//                String bg = switch (r.response) {
//                    case CORRECT_LOCATION -> BG_GREEN;
//                    case WRONG_LOCATION   -> BG_YELLOW;
//                    case WRONG_LETTER     -> BG_GRAY;
//                };
//                System.out.print(bg + Character.toUpperCase(r.letter) + RESET);
//            }
//            System.out.println();
//
//            // win?
//            if (guess.equals(secret)) {
//                System.out.println("🎉 Solved in " + turn + " turns!");
//                break GAME;
//            }
//
//            // suggestions
//            List<String> cand = wordleMatches(history);
//            System.out.println("   Candidates left: " + cand.size());
//            System.out.println("   Examples: " +
//                    cand.stream().limit(8).collect(Collectors.joining(", ")));
//            System.out.println();
//        }
//
//        System.out.println("🔍 Secret was: " + secret.toUpperCase());
//    }


}