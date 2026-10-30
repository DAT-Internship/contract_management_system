package com.cms.demo.security;

public class StaffIdValidator {

    public static boolean isValid(String staffId) {
        // ၁။ ၈ လုံး ရှိမရှိ စစ်ဆေးခြင်း (ဥပမာ - 25-00001)
        if (staffId == null || staffId.length() != 8) {
            return false;
        }

        // ၂။ ပထမ ၂ လုံးက 25 (Male) သို့မဟုတ် 26 (Female) ဖြစ်မဖြစ် စစ်ဆေးခြင်း
        String genderPrefix = staffId.substring(0, 2);
        if (!genderPrefix.equals("25") && !genderPrefix.equals("26")) {
            return false;
        }

        // ၃။ တတိယမြောက် စာလုံးက '-' (ဒက်ရှ်) ဖြစ်မဖြစ် စစ်ဆေးခြင်း
        if (staffId.charAt(2) != '-') {
            return false;
        }

        // ၄။ ကျန်တဲ့ ၅ လုံးက ဂဏန်း ဟုတ်မဟုတ် စစ်ဆေးခြင်း (index 3 ကနေ စမယ်)
        String actualId = staffId.substring(3);
        return actualId.matches("\\d{5}");
    }
}