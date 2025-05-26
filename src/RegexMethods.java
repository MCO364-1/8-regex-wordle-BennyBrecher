public class RegexMethods {
    // first letter of proper noun ony should be capitalized followed by zero or more lowercase letters
    //also accepts proper nouns with two or more capital letters with whitespaces in between
    public static boolean properName(String s) {
        return s.matches("^[A-Z][a-z]*(?:\\s[A-Z][a-z]*)*$");
    }

    //optional +/- sign and then either a 0 or any number 1-9 with any amount of following digits. optionally can include decimal point with at least one digit following it
    //a number (integer or decimal, positive or negative)
    //accepts 12, 43.23, -34.5, +98.7, 0, 0.0230 (but not 023)
    public static boolean integer(String s) {
        return s.matches("^[+-]?(?:0|[1-9]\\d*)(?:\\.\\d+)?$");
    }

    //accepts (case insensitive) either a father/mother or any amount of great's followed by a grand father/mother
    public static boolean ancestor(String s) {
        return s.matches("^(?i)(?:father|mother|(?:great-)*grand(?:father|mother))$");
    }

    // a 10 letter case insensitive palindrome like "asdfggfdsa"
    // (case insensitive) checks if the second 5 elements are a mirrored version of the first 5
    public static boolean palindrome(String s) {
        return s.matches("(?i)^(.)(.)(.)(.)(.)\\5\\4\\3\\2\\1$");
    }
}
