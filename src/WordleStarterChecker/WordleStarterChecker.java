package WordleStarterChecker;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;

public class WordleStarterChecker {
    public static void main(String[] args) {
        /**
         * as discussed challenges.txt this is old code I wrote 2 yrs ago
         */
        HashSet<String> usedWords = new HashSet();

        try {
            Scanner fileScanner = new Scanner(new FileReader("src/WordleStarterChecker/CleanWordList.txt"));

            while(fileScanner.hasNext()) {
                usedWords.add(fileScanner.nextLine().toUpperCase());
            }
        } catch (IOException e) {
            System.err.println("Error occurred while reading the file: " + e.getMessage());
            return;
        }

        Scanner userInput = new Scanner(System.in);
        System.out.print("What Is Your Starter Word? ");

        String starterWord;
        for(starterWord = userInput.nextLine().toUpperCase(); usedWords.contains(starterWord); starterWord = userInput.nextLine().toUpperCase()) {
            System.out.println("Sorry, It Appears That " + starterWord + " Has Already Been Used As A Wordle");
            System.out.print("Enter Your New Starter Word: ");
        }

        System.out.print("You're Good To Go, " + starterWord + " Has Not Yet Been Used As A Wordle");
        userInput.close();
    }

    public static void cleanWordList() {
        String inputFileName = "src/WordleStarterChecker/PastWordles.txt";
        String outputFileName = "src/WordleStarterChecker/CleanWordList.txt";

        try (
                Scanner fileScanner = new Scanner(new FileReader(inputFileName));
                PrintWriter writer = new PrintWriter(outputFileName);
        ) {
            while(fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] parts = line.split(" #");
                writer.println(parts[0]);
            }

            System.out.println("Word list cleaned and written to " + outputFileName);
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

    }
}
