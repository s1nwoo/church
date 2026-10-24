package com.banghwa.bibleppt;

import com.banghwa.bibleppt.BiblePptService.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class BiblePptServiceTest {
    private static BiblePptService service;
    @BeforeAll static void setup() throws Exception { service = new BiblePptService(); }

    @Test void rangePickerUsesOnlyExistingChaptersAndVerses() {
        var genesis = service.ranges("창");
        assertThat(genesis).hasSize(50);
        assertThat(genesis.get(1)).hasSize(31).contains(1, 31).doesNotContain(32);
        assertThat(service.ranges("시").get(119)).hasSize(176);
        assertThatThrownBy(() -> service.ranges("없는책")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void buildsRealVersesAndRejectsPartialOrUnboundedInput() {
        var slides = service.build(new BuildRequest("창1:1-3,5 요3:16", 3));
        assertThat(slides).hasSizeGreaterThanOrEqualTo(5);
        assertThat(slides.get(0).body()).startsWith("1 태초에");
        assertThat(slides).extracting(s -> s.title()).contains("창세기 1:5", "요한복음 3:16");
        for (String invalid : List.of("창1:1 요999:1", "창1:1-2147483647", "창1:1,", "창1:1-", "창1:0", "")) {
            assertThatThrownBy(() -> service.build(new BuildRequest(invalid, 3)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void exportsEditedSlidesWithPerSlideStylesAndTemplateDimensions() throws Exception {
        var metadata = service.metadata();
        var base = metadata.defaultStyle();
        var edited = new Style(28, 32, base.title(), base.body());
        var deck = new Deck(List.of(new Slide("창1:1, 창1:2", "1 첫 줄\n2 둘째 줄", edited),
                new Slide("요3:16", "16 하나님이 세상을", null)));
        byte[] bytes = service.download(deck);
        try (var ppt = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
            assertThat(ppt.getSlides()).hasSize(2);
            assertThat(ppt.getPageSize().width).isEqualTo((int) metadata.width());
            assertThat(ppt.getPageSize().height).isEqualTo((int) metadata.height());
            var shapes = ppt.getSlides().get(0).getShapes().stream().filter(s -> s instanceof XSLFTextShape)
                    .map(s -> (XSLFTextShape) s).toList();
            assertThat(shapes).extracting(XSLFTextShape::getText).contains("창세기 1:1, 창세기 1:2", "1 첫 줄\n2 둘째 줄");
            var body = shapes.stream().filter(s -> s.getText().startsWith("1 첫 줄")).findFirst().orElseThrow();
            assertThat(body.getTextParagraphs().get(0).getTextRuns().get(0).getFontSize()).isEqualTo(32);
            assertThat(body.getAnchor().getX()).isCloseTo(base.body().x(), within(.01));
        }
    }

    @Test void fullBookNamesAppearInGeneratedAndLegacyMergedPptTitles() throws Exception {
        var built = service.build(new BuildRequest("창1:1 왕상1:1 대상1:1", 0));
        assertThat(built).extracting(s -> s.title()).containsExactly("창세기 1:1", "열왕기상 1:1", "역대상 1:1");
        var titles = List.of("창1:1-3", "왕상1:1, 대상1:1", "열왕기상 1:1-2", "예배 말씀");
        var deck = new Deck(titles.stream().map(t -> new Slide(t, "본문은 창1:1 그대로", null)).toList());
        try (var ppt = new XMLSlideShow(new ByteArrayInputStream(service.download(deck)))) {
            var exportedTitles = ppt.getSlides().stream().map(slide -> slide.getShapes().stream()
                    .filter(s -> s instanceof XSLFTextShape).map(s -> ((XSLFTextShape) s).getText())
                    .filter(t -> !t.equals("본문은 창1:1 그대로")).findFirst().orElseThrow()).toList();
            assertThat(exportedTitles).containsExactly("창세기 1:1-3", "열왕기상 1:1, 역대상 1:1", "열왕기상 1:1-2", "예배 말씀");
        }
        // Every book uses the existing data's full name, including overlapping abbreviations.
        for (var book : service.metadata().books()) {
            var slide = service.build(new BuildRequest(book.getAbbreviation() + "1:1", 0)).get(0);
            assertThat(slide.title()).isEqualTo(book.getName() + " 1:1");
        }
    }

    @Test void rendersKoreanPreviewWithSameAspectRatio() throws Exception {
        var content = service.build(new BuildRequest("요3:16", 3)).get(0);
        var slide = new Slide(content.title(), content.body(), null);
        var png = service.preview(slide);
        var image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(960);
        assertThat(image.getHeight()).isEqualTo((int) Math.round(960 * service.metadata().height() / service.metadata().width()));
        Path dir = Path.of("build", "bible-ppt-qa");
        Files.createDirectories(dir);
        Files.write(dir.resolve("preview.png"), png);
        Files.write(dir.resolve("sample.pptx"), service.download(new Deck(List.of(slide))));
    }

    @Test void rejectsInvalidLayoutsAndOversizedDecks() {
        var b = service.metadata().defaultStyle();
        assertThatThrownBy(() -> service.download(new Deck(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.preview(new Slide("창1:1", "본문", new Style(28, 900, b.title(), b.body()))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.preview(new Slide("창1:1", "본문",
                new Style(28, 32, new Box(-1, 0, 10, 10), b.body()))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
