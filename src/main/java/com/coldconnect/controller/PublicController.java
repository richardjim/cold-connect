package com.coldconnect.controller;

import com.coldconnect.entity.ImpactMetric;
import com.coldconnect.entity.LeadEnquiry;
import com.coldconnect.repository.ImpactMetricRepository;
import com.coldconnect.repository.LeadEnquiryRepository;
import com.coldconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Public", description = "Public endpoints — website lead capture and impact stats")
public class PublicController extends BaseController {

    private final LeadEnquiryRepository  leadRepository;
    private final ImpactMetricRepository impactRepository;

    public PublicController(UserRepository userRepository,
                            LeadEnquiryRepository leadRepository,
                            ImpactMetricRepository impactRepository) {
        super(userRepository);
        this.leadRepository   = leadRepository;
        this.impactRepository = impactRepository;
    }

    public record ContactLeadRequest(
            @NotBlank String name,
            @NotBlank String phone,
            String email,
            String organization,
            @NotBlank String persona,
            String region,
            String message,
            @NotBlank
            @Pattern(regexp = "^(accepted|declined)$",
                    message = "Consent must be: accepted or declined")
            String consentStatus
    ) {}

    public record PartnerLeadRequest(
            @NotBlank String name,
            @NotBlank String organization,
            @NotBlank String partnerType,
            String region,
            String message,
            String phone,
            @Email String email,
            @NotBlank
            @Pattern(regexp = "^(accepted|declined)$",
                    message = "Consent must be: accepted or declined")
            String consentStatus
    ) {}

    @Operation(
            summary = "Contact page lead capture — public",
            description = "Personas: FARMER, COOPERATIVE, FUNDER, BUYER, OTHER"
    )
    @PostMapping("/v1/leads/contact")
    public ResponseEntity<Map<String, Object>> contactLead(
            @Valid @RequestBody ContactLeadRequest req) {

        LeadEnquiry lead = new LeadEnquiry();
        lead.setName(req.name());
        lead.setPhone(req.phone());
        lead.setEmail(req.email());
        lead.setOrganization(req.organization());
        lead.setPersona(req.persona());
        lead.setRegion(req.region());
        lead.setMessage(req.message());
        lead.setConsentStatus(req.consentStatus());
        lead.setSourceType("CONTACT");
        leadRepository.save(lead);

        return ResponseEntity.ok(Map.of(
                "leadId",  lead.getId(),
                "status",  "NEW",
                "message", "Thank you! We will be in touch soon."
        ));
    }

    @Operation(
            summary = "Partners/funders lead capture — public",
            description = "Partner types: FUNDER, LENDER, BUYER, HUB_HOST, DISTRIBUTOR"
    )
    @PostMapping("/v1/leads/partners")
    public ResponseEntity<Map<String, Object>> partnerLead(
            @Valid @RequestBody PartnerLeadRequest req) {

        LeadEnquiry lead = new LeadEnquiry();
        lead.setName(req.name());
        lead.setOrganization(req.organization());
        lead.setPartnerType(req.partnerType());
        lead.setRegion(req.region());
        lead.setMessage(req.message());
        lead.setPhone(req.phone());
        lead.setEmail(req.email());
        lead.setConsentStatus(req.consentStatus());
        lead.setSourceType("PARTNER");
        leadRepository.save(lead);

        return ResponseEntity.ok(Map.of(
                "partnerId", lead.getId(),
                "status",    "NEW",
                "message",   "Thank you for your interest! Our partnerships team will reach out."
        ));
    }

    @Operation(
            summary = "Public impact summary — website stats",
            description = "Labels pilots vs projections. No auth required."
    )
    @GetMapping("/v1/public/impact/summary")
    public ResponseEntity<Map<String, Object>> getPublicImpact(
            @RequestParam(required = false) String region) {

        List<ImpactMetric> metrics = impactRepository.findAll();

        double foodSaved  = metrics.stream()
                .mapToDouble(m -> m.getFoodSavedKg() != null ? m.getFoodSavedKg() : 0)
                .sum();

        double co2Avoided = metrics.stream()
                .mapToDouble(m -> m.getCo2AvoidedKg() != null ? m.getCo2AvoidedKg() : 0)
                .sum();

        double solarKwh   = metrics.stream()
                .mapToDouble(m -> m.getSolarCoolingKwh() != null ? m.getSolarCoolingKwh() : 0)
                .sum();

        return ResponseEntity.ok(Map.of(
                "status",           "PILOT",
                "region",           region != null ? region : "Jos",
                "foodSavedKg",      foodSaved,
                "co2AvoidedKg",     co2Avoided,
                "solarCoolingKwh",  solarKwh,
                "usersServed",      metrics.size(),
                "treesEquivalent",  co2Avoided / 21.0,
                "disclaimer",       "Figures are pilot estimates. Projections are indicative only."
        ));
    }

    @Operation(
            summary = "Get website page content by slug — public",
            description = "Slugs: home, about, impact, partners, contact"
    )
    @GetMapping("/web/pages/{slug}")
    public ResponseEntity<Map<String, Object>> getPage(@PathVariable String slug) {
        return ResponseEntity.ok(Map.of(
                "slug",    slug,
                "title",   getTitleForSlug(slug),
                "status",  "published",
                "seoMeta", Map.of(
                        "title",       "Cold Connect — " + getTitleForSlug(slug),
                        "description", "Truewatt Cold Connect — Nigeria's cold chain platform"
                )
        ));
    }

    private String getTitleForSlug(String slug) {
        return switch (slug) {
            case "home"     -> "Cold Connect by TrueWatt";
            case "about"    -> "About Cold Connect";
            case "impact"   -> "Our Impact";
            case "partners" -> "Partners & Funders";
            case "contact"  -> "Contact Us";
            default         -> slug;
        };
    }
}