package blackjack;

import java.io.Serializable;

/**
 * @author Tanner Lisonbee
 */
public class Card implements Serializable
{
    public enum Suit { HEARTS, SPADES, CLUBS, DIAMONDS }
    public enum Number 
    { 
        ACE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), 
        NINE(9), TEN(10), JACK(10), QUEEN(10), KING(10);
        
        final int value;
        Number (int value) 
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
    }
    
    private Suit suit;
    private Number number;
    private String unicode;//To disply a card, simply call this string value.
    
    public Card(Suit suit, Number number)
    {
        this.suit = suit;
        this.number = number;
        char unicodeRep;
        switch (suit) {
            case SPADES:
                unicodeRep = '\uDCA0';
                break;
            case HEARTS:
                unicodeRep = '\uDCB0';
                break;
            case DIAMONDS:
                unicodeRep = '\uDCC0';
                break;
            default:
                unicodeRep = '\uDCD0';
                break;
        }
        switch (number){
            case JACK:
                unicodeRep++;
                break;
            case QUEEN:
                unicodeRep+=2;
                break;
            case KING:
                unicodeRep+=3;
                break;
        }
        unicodeRep += number.value;
        this.unicode = "\uD83C" + unicodeRep;
    }

    public Suit getSuit() 
    {
        return suit;
    }

    public Number getNumber() 
    {
        return number;
    }
    
    public String getUnicode()
    {
        return unicode;
    }
    
    @Override
    public String toString()
    {
        return number + " of " + suit;
    }
}
