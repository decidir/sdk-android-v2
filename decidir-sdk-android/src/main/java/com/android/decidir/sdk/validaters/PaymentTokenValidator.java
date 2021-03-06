package com.android.decidir.sdk.validaters;

import android.content.Context;

import com.android.decidir.sdk.R;
import com.android.decidir.sdk.dto.PaymentTokenWithCardToken;
import com.android.decidir.sdk.dto.PaymentToken;
import com.android.decidir.sdk.dto.PaymentError;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by biandra on 30/09/16.
 */
public class PaymentTokenValidator {

    public static final String VISA = "visa";
    public static final String MASTERCARD = "mastercard";
    public static final String AMEX = "amex";
    public static final String CARTE_BLANCHE_CARD = "Carte Blanche Card";
    public static final String DISCOVER = "discover";
    public static final String JCB = "jcb";
    public static final String VISAMASTER = "visamaster";
    public static final String INSTA = "insta";
    public static final String LASER = "laser";
    public static final String MAESTRO = "maestro";
    public static final String SOLO = "solo";
    public static final String SWITCH = "switch";
    public static final String UNION = "union";
    public static final String KOREAN = "korean";
    public static final String BCGLOBAL = "bcglobal";
    public static final String NARANJA = "naranja";
    private Context context;

    public Map<PaymentError, String> validate(PaymentToken paymentToken, Context context){
        this.context = context;
        Map<PaymentError, String> validation = new HashMap<>();
        creditCardNumberValidator(paymentToken.getCard_number(), validation);
        cvcValidate(paymentToken.getSecurity_code(), validation);
        expiryDateValidator(paymentToken.getCard_expiration_month(), paymentToken.getCard_expiration_year(), validation);
        cardHolderNameValidator(paymentToken.getCard_holder_name(), validation);
        typeIdValidator(paymentToken.getCard_holder_identification().getNumber(), validation);
        portNumberValidate(paymentToken.getCard_holder_door_number(), validation);
        return validation;
    }

    private void typeIdValidator(String numberId, Map<PaymentError, String> validation) {
        if (!matcheNumber(numberId)) {
            validation.put(PaymentError.TYPE_ID, context.getResources().getString(R.string.dni_validate));
        }
    }

    private void cardHolderNameValidator(String cardHolderName, Map<PaymentError, String> validation) {
        if (cardHolderName.isEmpty()){
            validation.put(PaymentError.CARD_HOLDER_NAME, context.getResources().getString(R.string.card_holder_name_validate));
        }
    }

    private void expiryDateValidator(String cardExpirationMonth, String cardExpirationYear, Map<PaymentError, String> validation) {
        boolean validCEM = validExpirationMonth(cardExpirationMonth);
        boolean validCEY = validExpirationYear(cardExpirationYear);
        if (!validCEM) {
            validation.put(PaymentError.CARD_EXPIRATION_MONTH, context.getResources().getString(R.string.card_expiration_month_validate));
        }
        if (!validCEY) {
            validation.put(PaymentError.CARD_EXPIRATION_YEAR, context.getResources().getString(R.string.card_expiration_year_validate));
        }
        if(validCEM && validCEY){
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/yyyy");
            Calendar cal = Calendar.getInstance();
            try {
                Date currentDate = dateFormat.parse(String.valueOf(cal.get(Calendar.MONTH) + 1) + "/" + String.valueOf(cal.get(Calendar.YEAR)));
                Date expirationDate = dateFormat.parse(cardExpirationMonth + "/" + "20" + cardExpirationYear);
                if (expirationDate.before(currentDate)) {
                    validation.put(PaymentError.CARD_EXPIRATION, context.getResources().getString(R.string.card_expiration_validate));
                }
            } catch (ParseException e) {
                validation.put(PaymentError.CARD_EXPIRATION, context.getResources().getString(R.string.card_expiration_validate));

            }
        }
    }

    private boolean validExpirationMonth(String cardExpirationMonth) {
        return !cardExpirationMonth.isEmpty() && cardExpirationMonth.matches("1[0-2]|0[1-9]");
    }

    private boolean validExpirationYear(String cardExpirationYear) {
        return !cardExpirationYear.isEmpty() && cardExpirationYear.matches("\\d{2}");
    }

    private void creditCardNumberValidator(String card_number, Map<PaymentError, String> validation) {
        String cardType = getCardType(card_number);
        boolean validLuhn = luhnValidator(card_number);
        if (!matcheNumber(card_number) ||
                (!validLuhn && !isNaranja(cardType)) ||
                !validLuhn) {
            validation.put(PaymentError.CARD_NUMBER, context.getResources().getString(R.string.card_number_validate));
        }
    }

    private boolean isNaranja(String cardType) {
        return NARANJA.equals(cardType);
    }

    private String getCardType(String cardNumber) {
        List<IssuingNetworks> issuingNetworkses = getIssuingNetworks();
        for (int i=0; i>= issuingNetworkses.size(); i++){
            if (cardNumber.matches(issuingNetworkses.get(i).regEx)){
                return issuingNetworkses.get(i).name;
            }
        }
        return null;
    }

    public Map<PaymentError, String> validate(PaymentTokenWithCardToken paymentTokenWithCardToken, Context context){
        this.context = context;
        Map<PaymentError, String> validation = new HashMap<>();
        cvcValidate(paymentTokenWithCardToken.getSecurity_code(), validation);
        return validation;
    }

    private void cvcValidate(String securityCode, Map<PaymentError, String> validation) {
        if (!matcheNumber(securityCode)) {
            validation.put(PaymentError.SECURITY_CODE, context.getResources().getString(R.string.cvc_validate));
        }
    }

    private void portNumberValidate(Integer portNumber, Map<PaymentError, String> validation) {
        /*if (portNumber != null && portNumber.length() > 6){
            validation.put(PaymentError.PORT_NUMBER, context.getResources().getString(R.string.port_number_validate));
        }*/
    }

    private boolean matcheNumber(String value){
        return !value.isEmpty() && value.matches("\\d+");
    }


    private boolean luhnValidator(String ccNumber)
    {
        ccNumber.toString().replace("[ .-]", "");
        int total = 0;
        boolean alternate = false;
        for (int i = ccNumber.length() - 1; i >= 0; i--)
        {
            int curDigit = Integer.parseInt(ccNumber.substring(i, i + 1));
            if (alternate)
            {
                curDigit *= 2;
                if (curDigit > 9)
                    curDigit -= 9;
            }
            total += curDigit;
            alternate = !alternate;
        }
        return total % 10 == 0;
    }


    private List<IssuingNetworks> getIssuingNetworks() {
        List<IssuingNetworks> issuingNetworkses = new ArrayList<>();
        issuingNetworkses.add(new IssuingNetworks(VISA, "^4[0-9]{12}(?:[0-9]{3})?$"));
        issuingNetworkses.add(new IssuingNetworks(MASTERCARD, "^5[1-5][0-9]{14}$"));
        issuingNetworkses.add(new IssuingNetworks(AMEX, "^3[47][0-9]{13}$"));
        issuingNetworkses.add(new IssuingNetworks(CARTE_BLANCHE_CARD, "^389[0-9]{11}$"));
        issuingNetworkses.add(new IssuingNetworks(DISCOVER, "^65[4-9][0-9]{13}|64[4-9][0-9]{13}|6011[0-9]{12}|(622(?:12[6-9]|1[3-9][0-9]|[2-8][0-9][0-9]|9[01][0-9]|92[0-5])[0-9]{10})$"));
        issuingNetworkses.add(new IssuingNetworks(JCB, "^(?:2131|1800|35\\d{3})\\d{11}$"));
        issuingNetworkses.add(new IssuingNetworks(VISAMASTER, "^(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14})$"));
        issuingNetworkses.add(new IssuingNetworks(INSTA, "^63[7-9][0-9]{13}$"));
        issuingNetworkses.add(new IssuingNetworks(LASER, "^(6304|6706|6709|6771)[0-9]{12,15}$"));
        issuingNetworkses.add(new IssuingNetworks(MAESTRO, "^(5018|5020|5038|6304|6759|6761|6763)[0-9]{8,15}$"));
        issuingNetworkses.add(new IssuingNetworks(SOLO, "^(6334|6767)[0-9]{12}|(6334|6767)[0-9]{14}|(6334|6767)[0-9]{15}$/"));
        issuingNetworkses.add(new IssuingNetworks(SWITCH, "^(4903|4905|4911|4936|6333|6759)[0-9]{12}|(4903|4905|4911|4936|6333|6759)[0-9]{14}|(4903|4905|4911|4936|6333|6759)[0-9]{name: {15}|564182[0-9]{10}|564182[0-9]{12}|564182[0-9]{13}|633110[0-9]{10}|633110[0-9]{12}|633110[0-9]{13}$"));
        issuingNetworkses.add(new IssuingNetworks(UNION, "^(62[0-9]{14,17})$"));
        issuingNetworkses.add(new IssuingNetworks(KOREAN, "^9[0-9]{15}$"));
        issuingNetworkses.add(new IssuingNetworks(BCGLOBAL, "^(6541|6556)[0-9]{12}$"));
        issuingNetworkses.add(new IssuingNetworks(NARANJA, "^589562[0-9]{10}$"));
        return issuingNetworkses;
    }

    class IssuingNetworks {
        String name;
        String regEx;
        public IssuingNetworks(String name, String regEx){
            this.name = name;
            this.regEx = regEx;
        }
    }

}
