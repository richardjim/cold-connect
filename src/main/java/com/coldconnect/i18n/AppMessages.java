package com.coldconnect.i18n;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AppMessages {

    public enum Key {
        OTP_SENT,
        OTP_INVALID,
        OTP_EXPIRED,
        OTP_VERIFIED,
        SIGNUP_SUCCESS,
        LOGIN_NEXT_STEP,
        PHONE_ALREADY_REGISTERED,
        PHONE_NOT_REGISTERED,
        VOICE_OTP_INITIATED,
        PROFILE_UPDATED,
        BOOKING_CREATED,
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        BOOKING_NOT_PENDING,
        BOOKING_NOT_FOUND,
        CART_ITEM_ADDED,
        CART_ITEM_UPDATED,
        CART_ITEM_REMOVED,
        CART_CLEARED,
        WALLET_TOPUP_SUCCESS,
        WALLET_WITHDRAW_SUCCESS,
        INSUFFICIENT_BALANCE,
        WAITLIST_JOINED,
        WAITLIST_ALREADY_ON,
        WAITLIST_CANCELLED,
        HUB_NOT_FOUND,
        LOT_NOT_AVAILABLE,
        LOGOUT_SUCCESS,
        EMAIL_VERIFIED,
        EMAIL_ALREADY_VERIFIED,
        VERIFICATION_SENT,
        PASSWORD_RESET_SENT,
        PASSWORD_RESET_SUCCESS,
        SUPPORT_CASE_CREATED,
        NOTIFICATION_READ
    }

    private static final Map<Key, Map<String, String>> MESSAGES = Map.ofEntries(

            Map.entry(Key.OTP_SENT, Map.of(
                    "en",  "OTP sent to your phone. Valid for 10 minutes.",
                    "ha",  "An aika lambar sirri zuwa wayar ku. Tana da inganci na minti 10.",
                    "yo",  "A ti fi koodu ranṣẹ si foonu rẹ. O wulo fun iṣẹju 10.",
                    "ig",  "Ezigara koodu gị n'ekwentị gị. Ọ dị maka nkeji 10.",
                    "pcm", "We don send OTP go your phone. E go expire for 10 minutes."
            )),

            Map.entry(Key.OTP_INVALID, Map.of(
                    "en",  "Invalid OTP code. Please try again.",
                    "ha",  "Lambar sirri ba daidai ba ce. Don Allah sake gwadawa.",
                    "yo",  "Koodu OTP kò tọ. Jọwọ gbiyanju lẹẹkansi.",
                    "ig",  "Koodu OTP adịghị mma. Biko nwaa ọzọ.",
                    "pcm", "Your OTP no correct. Try am again."
            )),

            Map.entry(Key.OTP_EXPIRED, Map.of(
                    "en",  "OTP has expired. Please request a new one.",
                    "ha",  "Lambar sirri ta ƙare. Don Allah nemi sabuwa.",
                    "yo",  "Koodu OTP ti pari. Jọwọ beere tuntun.",
                    "ig",  "Koodu OTP agwụla. Biko rịọ ọzọ.",
                    "pcm", "Your OTP don expire. Request new one."
            )),

            Map.entry(Key.OTP_VERIFIED, Map.of(
                    "en",  "OTP verified successfully.",
                    "ha",  "An tabbatar da lambar sirri cikin nasara.",
                    "yo",  "A ti jẹrisi koodu OTP.",
                    "ig",  "Ejiri ike gosipụta koodu OTP.",
                    "pcm", "Your OTP don verify. Welcome!"
            )),

            Map.entry(Key.SIGNUP_SUCCESS, Map.of(
                    "en",  "Account created. Check your phone for OTP.",
                    "ha",  "An ƙirƙiri asusun. Duba wayar ku don lambar sirri.",
                    "yo",  "A ti ṣẹda akọọlẹ. Ṣayẹwo foonu rẹ fun OTP.",
                    "ig",  "Emepụtara akaụntụ. Lelee ekwentị gị maka OTP.",
                    "pcm", "Account don ready. Check your phone for OTP."
            )),

            Map.entry(Key.LOGIN_NEXT_STEP, Map.of(
                    "en",  "OTP sent. Enter it to log in.",
                    "ha",  "An aika lambar sirri. Shigar da ita don shiga.",
                    "yo",  "A ti fi OTP ranṣẹ. Tẹ sii lati wọle.",
                    "ig",  "Ezigara OTP. Tinye ya iji banye.",
                    "pcm", "OTP don go. Enter am to login."
            )),

            Map.entry(Key.PHONE_ALREADY_REGISTERED, Map.of(
                    "en",  "Phone number already registered. Use login instead.",
                    "ha",  "Lambar wayar an riga an yi rajista. Yi amfani da shiga.",
                    "yo",  "Nọmba foonu ti forukọsilẹ tẹlẹ. Lo wiwọle dipo.",
                    "ig",  "Nọmba ekwentị edegọla. Jiri nbanye kama.",
                    "pcm", "This phone number don register before. Use login."
            )),

            Map.entry(Key.PHONE_NOT_REGISTERED, Map.of(
                    "en",  "Phone number not registered. Please sign up first.",
                    "ha",  "Lambar wayar ba a yi rajista ba. Don Allah fara yin rajista.",
                    "yo",  "Nọmba foonu ko forukọsilẹ. Jọwọ forukọsilẹ.",
                    "ig",  "Nọmba ekwentị adereghị. Biko deere aha gị.",
                    "pcm", "This phone number no dey. Please sign up first."
            )),

            Map.entry(Key.VOICE_OTP_INITIATED, Map.of(
                    "en",  "Voice OTP call initiated. Please answer your phone.",
                    "ha",  "An fara kiran murya. Don Allah amsa wayar ku.",
                    "yo",  "A ti bẹrẹ ipe ohun. Jọwọ dahun foonu rẹ.",
                    "ig",  "Oku olu OTP amalitela. Biko zaa ekwentị gị.",
                    "pcm", "We don call your phone. Answer the call."
            )),

            Map.entry(Key.PROFILE_UPDATED, Map.of(
                    "en",  "Profile updated successfully.",
                    "ha",  "An sabunta bayanin cikin nasara.",
                    "yo",  "A ti ṣe imudojuiwọn profaili.",
                    "ig",  "Emelitere profaịlụ nke ọma.",
                    "pcm", "Your profile don update."
            )),

            Map.entry(Key.BOOKING_CREATED, Map.of(
                    "en",  "Booking created successfully. Please confirm to proceed.",
                    "ha",  "An ƙirƙiri kama cikin nasara. Don Allah tabbatar da ci gaba.",
                    "yo",  "A ti ṣẹda iṣẹ aṣẹ. Jọwọ jẹrisi lati tẹsiwaju.",
                    "ig",  "Emepụtara ntinye akwụkwọ nke ọma. Biko nkwenye ịga n'ihu.",
                    "pcm", "Booking don ready. Confirm am to continue."
            )),

            Map.entry(Key.BOOKING_CONFIRMED, Map.of(
                    "en",  "Booking confirmed.",
                    "ha",  "An tabbatar da kama.",
                    "yo",  "A ti jẹrisi iṣẹ aṣẹ.",
                    "ig",  "Akwụkwọ ntinye anọkwaala.",
                    "pcm", "Booking don confirm."
            )),

            Map.entry(Key.BOOKING_CANCELLED, Map.of(
                    "en",  "Booking cancelled.",
                    "ha",  "An soke kama.",
                    "yo",  "A ti fagilee iṣẹ aṣẹ.",
                    "ig",  "Akwụkwọ ntinye ewepụla.",
                    "pcm", "Booking don cancel."
            )),

            Map.entry(Key.BOOKING_NOT_PENDING, Map.of(
                    "en",  "Booking cannot be modified in its current status.",
                    "ha",  "Ba za a iya canza kama a halin yanzu ba.",
                    "yo",  "Ko ṣee ṣe lati yi iṣẹ aṣẹ ni ipo lọwọlọwọ.",
                    "ig",  "Enweghị ike ịgbanwe akwụkwọ ntinye n'ọnọdụ ya ugbu a.",
                    "pcm", "You no fit change this booking again."
            )),

            Map.entry(Key.BOOKING_NOT_FOUND, Map.of(
                    "en",  "Booking not found.",
                    "ha",  "Ba a sami kama ba.",
                    "yo",  "Ko ri iṣẹ aṣẹ.",
                    "ig",  "Ahụghị akwụkwọ ntinye.",
                    "pcm", "We no find this booking."
            )),

            Map.entry(Key.CART_ITEM_ADDED, Map.of(
                    "en",  "Item added to cart.",
                    "ha",  "An ƙara abu zuwa kwandon.",
                    "yo",  "A ti ṣafikun nkan si agbọn.",
                    "ig",  "Etinye ihe na cart.",
                    "pcm", "Item don enter your cart."
            )),

            Map.entry(Key.CART_ITEM_UPDATED, Map.of(
                    "en",  "Cart item updated.",
                    "ha",  "An sabunta abu a kwandon.",
                    "yo",  "A ti ṣe imudojuiwọn nkan ninu agbọn.",
                    "ig",  "Emelitere ihe na cart.",
                    "pcm", "Cart item don update."
            )),

            Map.entry(Key.CART_ITEM_REMOVED, Map.of(
                    "en",  "Item removed from cart.",
                    "ha",  "An cire abu daga kwandon.",
                    "yo",  "A ti yọ nkan kuro ninu agbọn.",
                    "ig",  "Ewepụ ihe na cart.",
                    "pcm", "Item don comot from your cart."
            )),

            Map.entry(Key.CART_CLEARED, Map.of(
                    "en",  "Cart cleared.",
                    "ha",  "An share kwandon.",
                    "yo",  "A ti pa agbọn.",
                    "ig",  "Ewezụ cart.",
                    "pcm", "Cart don clear."
            )),

            Map.entry(Key.WALLET_TOPUP_SUCCESS, Map.of(
                    "en",  "Wallet topped up successfully.",
                    "ha",  "An cika walat cikin nasara.",
                    "yo",  "A ti kun apo-owo rẹ.",
                    "ig",  "Etinye ego na wallet nke ọma.",
                    "pcm", "Your wallet don top up."
            )),

            Map.entry(Key.WALLET_WITHDRAW_SUCCESS, Map.of(
                    "en",  "Withdrawal successful.",
                    "ha",  "Fitar kudi ya yi nasara.",
                    "yo",  "Yiyọ owo ti ṣaṣeyọri.",
                    "ig",  "Iwepụ ego gara nke ọma.",
                    "pcm", "Withdrawal don go through."
            )),

            Map.entry(Key.INSUFFICIENT_BALANCE, Map.of(
                    "en",  "Insufficient wallet balance.",
                    "ha",  "Kudin walat ba su isa ba.",
                    "yo",  "Owo apo-owo ko to.",
                    "ig",  "Ego n'ime wallet ezughị.",
                    "pcm", "Your wallet balance no reach."
            )),

            Map.entry(Key.WAITLIST_JOINED, Map.of(
                    "en",  "You have been added to the waitlist. We will notify you when space is available.",
                    "ha",  "An ƙara ku cikin jerin jira. Za mu sanar da ku lokacin da akwai sarari.",
                    "yo",  "A ti ṣafikun ọ si atokọ idaduro. A o fi ọ leti nigbati aaye ba wa.",
                    "ig",  "Etinyere gị na ndepụta nche. Anyị ga-enyere gị ọkwa mgbe ohere dị.",
                    "pcm", "We don add you to waitlist. We go tell you when space dey."
            )),

            Map.entry(Key.WAITLIST_ALREADY_ON, Map.of(
                    "en",  "You are already on the waitlist for this hub.",
                    "ha",  "Kuna riga kuna jerin jira don wannan cibiya.",
                    "yo",  "O ti wa ninu atokọ idaduro fun ibudo yii.",
                    "ig",  "Ị nọdịkwa na ndepụta nche maka ụlọ ọrụ a.",
                    "pcm", "You don dey waitlist for this hub before."
            )),

            Map.entry(Key.WAITLIST_CANCELLED, Map.of(
                    "en",  "Waitlist entry cancelled.",
                    "ha",  "An soke shigar jerin jira.",
                    "yo",  "A ti fagilee titẹsi atokọ idaduro.",
                    "ig",  "Ewepụ ndepụta nche.",
                    "pcm", "Waitlist entry don cancel."
            )),

            Map.entry(Key.HUB_NOT_FOUND, Map.of(
                    "en",  "Hub not found.",
                    "ha",  "Ba a sami cibiya ba.",
                    "yo",  "Ko ri ibudo.",
                    "ig",  "Ahụghị ụlọ ọrụ.",
                    "pcm", "We no find this hub."
            )),

            Map.entry(Key.LOT_NOT_AVAILABLE, Map.of(
                    "en",  "This lot is no longer available.",
                    "ha",  "Wannan kaya ba ya samuwa kuma.",
                    "yo",  "Ẹru yii ko si mọ.",
                    "ig",  "Ngwongwo a adịghịzi ọnụ.",
                    "pcm", "This lot no dey again."
            )),

            Map.entry(Key.LOGOUT_SUCCESS, Map.of(
                    "en",  "Logged out successfully.",
                    "ha",  "An fita cikin nasara.",
                    "yo",  "A ti jade ni aṣeyọri.",
                    "ig",  "Apụọla nke ọma.",
                    "pcm", "You don logout."
            )),

            Map.entry(Key.EMAIL_VERIFIED, Map.of(
                    "en",  "Email verified. You can now log in.",
                    "ha",  "An tabbatar da imel. Yanzu zaku iya shiga.",
                    "yo",  "A ti jẹrisi imeeli. O le wọle bayi.",
                    "ig",  "Ejiri ike gosipụta email. Ị nwere ike banye ugbu a.",
                    "pcm", "Email don verify. You fit login now."
            )),

            Map.entry(Key.EMAIL_ALREADY_VERIFIED, Map.of(
                    "en",  "Email is already verified.",
                    "ha",  "An riga an tabbatar da imel.",
                    "yo",  "A ti jẹrisi imeeli tẹlẹ.",
                    "ig",  "Ejisiri ike gosipụtara email.",
                    "pcm", "Email don already verify."
            )),

            Map.entry(Key.VERIFICATION_SENT, Map.of(
                    "en",  "Verification code sent to your email.",
                    "ha",  "An aika lambar tabbatarwa zuwa imelku.",
                    "yo",  "A ti fi koodu jẹrisi ranṣẹ si imeeli rẹ.",
                    "ig",  "Ezigara koodu nkwenye gị na email gị.",
                    "pcm", "We don send verification code to your email."
            )),

            Map.entry(Key.PASSWORD_RESET_SENT, Map.of(
                    "en",  "If that email is registered, a reset code has been sent.",
                    "ha",  "Idan an yi rajista da wannan imel, an aika lambar sake saita.",
                    "yo",  "Ti imeeli naa ba forukọsilẹ, a ti fi koodu atunṣe ranṣẹ.",
                    "ig",  "Ọ bụrụ na ejikọtara email ahụ, ezigara koodu ntọgharị.",
                    "pcm", "If that email dey, we don send reset code."
            )),

            Map.entry(Key.PASSWORD_RESET_SUCCESS, Map.of(
                    "en",  "Password reset successful. Please log in.",
                    "ha",  "Sake saita kalmar sirri ya yi nasara. Don Allah shiga.",
                    "yo",  "Atunṣe ọrọ aṣina ti ṣaṣeyọri. Jọwọ wọle.",
                    "ig",  "Ntọgharị paswọọdụ gara nke ọma. Biko banye.",
                    "pcm", "Password don reset. You fit login now."
            )),

            Map.entry(Key.SUPPORT_CASE_CREATED, Map.of(
                    "en",  "Support case created. Our team will respond within the SLA window.",
                    "ha",  "An ƙirƙiri shari'ar tallafi. Ƙungiyarmu za ta amsa cikin lokaci.",
                    "yo",  "A ti ṣẹda ọran atilẹyin. Ẹgbẹ wa yoo dahun laarin akoko SLA.",
                    "ig",  "Emepụtara ikpe nkwado. Ndị otu anyị ga-aza n'ime oge SLA.",
                    "pcm", "Support case don open. Our team go reply you soon."
            )),

            Map.entry(Key.NOTIFICATION_READ, Map.of(
                    "en",  "Notification marked as read.",
                    "ha",  "An yi alama sanarwa a matsayin da aka karanta.",
                    "yo",  "A ti samisi iwifunni bi ti ka.",
                    "ig",  "Akara ọkwa dị ka ọ gụọla.",
                    "pcm", "Notification don mark as read."
            ))
    );

    public String get(Key key, String language) {
        String lang = (language != null && !language.isBlank()) ? language : "en";
        Map<String, String> translations = MESSAGES.get(key);
        if (translations == null) return key.name();
        return translations.getOrDefault(lang, translations.getOrDefault("en", key.name()));
    }

    public String get(Key key) {
        return get(key, "en");
    }
}