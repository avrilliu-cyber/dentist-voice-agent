package com.autoaccru.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.autoaccru.dentistapi.patient.Patient;
import com.autoaccru.dentistapi.patient.PatientRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/voice")
public class VoiceController {

    private final PatientRepository patientRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ELEVEN_API_KEY}")
    private String elevenApiKey;

    public VoiceController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> handleVoiceUpload(@RequestParam("file") MultipartFile file) {
        try {
            String url = "https://api.elevenlabs.io/v1/speech-to-text";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("xi-api-key", elevenApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            body.add("model_id", "scribe_v1");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getBody() == null || !response.getBody().containsKey("text")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Speech recognition failed");
            }

            String transcript = (String) response.getBody().get("text");
            System.out.println("🗣️ Recognized text: " + transcript);

            Patient p = parsePatientFromText(transcript);

            // new or old patient
            boolean exists = patientRepository.findAll().stream().anyMatch(existing ->
                    normalizeDigits(existing.getPhoneNumber()).equals(normalizeDigits(p.getPhoneNumber())) &&
                            (existing.getFirstName().equalsIgnoreCase(p.getFirstName()) ||
                                    existing.getLastName().equalsIgnoreCase(p.getLastName()))
            );


            p.setNewPatient(!exists);

            patientRepository.save(p);
            return ResponseEntity.ok(p);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    private Patient parsePatientFromText(String text) {
        if (text == null || text.isEmpty()) return new Patient();

        text = text.toLowerCase()
                .replaceAll("[,.;!?]", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("adress", "address")
                .replaceAll("live on", "live at")
                .replaceAll("adress is", "address is")
                .trim();

        System.out.println("🧹 Cleaned: " + text);

        Patient p = new Patient();

        // full name matcher
        Pattern fullNamePattern = Pattern.compile("my name is\\s+([a-z]+)\\s+([a-z]+)");
        Matcher fullM = fullNamePattern.matcher(text);
        if (fullM.find()) {
            p.setFirstName(capitalize(fullM.group(1)));
            p.setLastName(capitalize(fullM.group(2)));
        } else {
            // first name only
            Pattern firstPattern = Pattern.compile("first name is\\s+([a-z]+)");
            Matcher fn = firstPattern.matcher(text);
            if (fn.find()) p.setFirstName(capitalize(fn.group(1)));

            // last name
            Pattern lastPattern = Pattern.compile("last name is\\s+([a-z]+)");
            Matcher ln = lastPattern.matcher(text);
            if (ln.find()) p.setLastName(capitalize(ln.group(1)));
        }

        // phone number:'()'' ''-'
        Pattern phonePattern = Pattern.compile(
                "(?:phone number is|my phone number is|my number is|contact number is|call me at)[\\s,:-]*([0-9\\(\\)\\s-]+)"
        );
        Matcher phoneM = phonePattern.matcher(text);
        if (phoneM.find()) {
            String phoneRaw = phoneM.group(1);
            String digits = phoneRaw.replaceAll("[^0-9]", ""); // 提取纯数字
            if (digits.length() >= 10) {
                digits = digits.substring(0, 10);
                p.setPhoneNumber(digits.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3"));
            } else {
                p.setPhoneNumber(null);
            }
        }

        // address: matching to the end of the sentence or 'and'
        Pattern addrPattern = Pattern.compile(
                "(?:my address is|address is|i live at|i live in)[\\s,:-]*([a-z0-9\\s]+?)(?=\\s*(?:and|$))"
        );
        Matcher addrM = addrPattern.matcher(text);
        if (addrM.find()) {
            String addr = addrM.group(1).trim();
            p.setAddress(capitalizeEachWord(cleanAddress(addr)));
        }

        // double check
        if (p.getFirstName() == null) p.setFirstName("Unknown");
        if (p.getLastName() == null) p.setLastName("Unknown");
        if (p.getAddress() == null) p.setAddress("Unspecified");
        if (p.getPhoneNumber() != null && p.getPhoneNumber().isBlank()) p.setPhoneNumber(null);

        System.out.println("✅ Parsed → phone: " + p.getPhoneNumber() + " | address: " + p.getAddress());
        return p;
    }


    private String normalizeNumbers(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("zero", "0").replaceAll("oh", "0")
                .replaceAll("one", "1")
                .replaceAll("two", "2").replaceAll("to", "2").replaceAll("too", "2")
                .replaceAll("three", "3")
                .replaceAll("four", "4").replaceAll("for", "4")
                .replaceAll("five", "5")
                .replaceAll("six", "6")
                .replaceAll("seven", "7")
                .replaceAll("eight", "8")
                .replaceAll("nine", "9");
    }

    private String cleanAddress(String text) {
        if (text == null) return null;
        return text.replaceAll(",", "").replaceAll("\\s+", " ").trim();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String capitalizeEachWord(String text) {
        if (text == null) return null;
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
        }
        return sb.toString().trim();
    }

    private String normalizeDigits(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

}
