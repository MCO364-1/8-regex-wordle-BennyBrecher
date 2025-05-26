import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class RegexMethodsTests {

    @Test
    void isProperName(){
        assertTrue(RegexMethods.properName("Benny"));
        assertFalse(RegexMethods.properName("benny"));
        assertTrue(RegexMethods.properName("I"));
        assertFalse(RegexMethods.properName("i"));
        assertTrue(RegexMethods.properName("Eifel Tower"));
        assertFalse(RegexMethods.properName("Eifel tower"));
        assertTrue(RegexMethods.properName("Statue Of Liberty"));
        assertFalse(RegexMethods.properName("Statue of Liberty"));
    }

    @Test
    void isInteger(){
        assertTrue(RegexMethods.integer("0"));
        assertTrue(RegexMethods.integer("0.0"));
        assertTrue(RegexMethods.integer("-0.0"));
        assertTrue(RegexMethods.integer("-9999999999.999999999999999"));
        assertTrue(RegexMethods.integer("+1234567891011121314151617181920.21222324252627282930"));
        assertTrue(RegexMethods.integer("0.230"));
        assertFalse(RegexMethods.integer("0230"));
        assertFalse(RegexMethods.integer("-.5"));
    }

    @Test
    void isAncestor(){
        assertTrue(RegexMethods.ancestor("father"));
        assertTrue(RegexMethods.ancestor("Father"));
        assertTrue(RegexMethods.ancestor("mother"));
        assertTrue(RegexMethods.ancestor("moTHeR"));
        assertFalse(RegexMethods.ancestor("cousin"));
        assertFalse(RegexMethods.ancestor("son"));
        assertTrue(RegexMethods.ancestor("great-grandfather"));
        assertTrue(RegexMethods.ancestor("grandmother"));
        assertTrue(RegexMethods.ancestor("Great-Great-Grandmother"));
        assertFalse(RegexMethods.ancestor("greatgrandfather"));
    }

    @Test
    void isPalindrome(){
        assertTrue(RegexMethods.palindrome("asdfggfdsa"));
        assertTrue(RegexMethods.palindrome("Pizzaazzip"));
        assertTrue(RegexMethods.palindrome("pizzaazziP"));
        assertTrue(RegexMethods.palindrome("PizzaazziP"));
        assertFalse(RegexMethods.palindrome("pizzazzip")); //palindrome shorter than 10 letters
        assertFalse(RegexMethods.palindrome("Bob"));
        assertFalse(RegexMethods.palindrome("toot"));
    }
}
