package com.banghwa.bibleppt;

import com.banghwa.bibleppt.config.AppConfig;
import com.banghwa.bibleppt.data.BibleRepository;
import com.banghwa.bibleppt.model.*;
import com.banghwa.bibleppt.parser.ReferenceParser;
import com.banghwa.bibleppt.ppt.PptGenerator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class BiblePptService {
    public static final int MAX_SLIDES = 200;
    private final BibleRepository repository;
    private final ReferenceParser parser;
    private final PptGenerator generator;
    private final Metadata metadata;

    public BiblePptService() throws Exception {
        var config = new AppConfig();
        repository = BibleRepository.load(config);
        parser = new ReferenceParser(repository);
        generator = new PptGenerator(config, repository);
        try (var font = new ClassPathResource("bible-ppt/Pretendard-Black.otf").getInputStream()) {
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, font));
        }
        try (var in = config.openTemplate(); var ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow(in)) {
            var original = Style.from(generator.readDefaultStyle());
            double width = ppt.getPageSize().width, height = ppt.getPageSize().height;
            // The desktop template has a title extending beyond the page and a one-line body
            // box that relies on overflow. Give the web editor visible, usable drag bounds.
            var title = fit(original.title(), width, height, false);
            var body = fit(original.body(), width, height, true);
            metadata = new Metadata(repository.getBooks(), ppt.getPageSize().width, ppt.getPageSize().height,
                    new Style(original.titleFontSize(), original.bodyFontSize(), title, body), MAX_SLIDES);
        }
    }

    private static Box fit(Box b, double width, double height, boolean body) {
        double x = Math.max(0, Math.min(width - 1, b.x()));
        double y = Math.max(0, Math.min(height - 1, b.y()));
        return new Box(x, y, Math.min(b.width(), width - x),
                body ? Math.max(1, height - y - 20) : Math.min(b.height(), height - y));
    }

    public Metadata metadata() { return metadata; }

    public java.util.Map<Integer, List<Integer>> ranges(String abbreviation) {
        var chapters = repository.getChapters(abbreviation);
        require(!chapters.isEmpty(), "성경 이름을 확인해 주세요.");
        var ranges = new java.util.LinkedHashMap<Integer, List<Integer>>();
        chapters.forEach(chapter -> ranges.put(chapter, repository.getVerses(abbreviation, chapter)));
        return java.util.Collections.unmodifiableMap(ranges);
    }

    public List<SlideContent> build(BuildRequest request) {
        require(request != null && request.reference() != null && !request.reference().isBlank(), "성경 구절을 입력해 주세요.");
        require(request.reference().length() <= 2000, "구절 입력은 2,000자 이내로 입력해 주세요.");
        require(request.lineLimit() >= 0 && request.lineLimit() <= 12, "줄 수는 0~12 사이로 입력해 주세요.");
        var parsed = parser.parse(request.reference());
        require(parsed.isValid(), parsed.validationMessage());
        require(parsed.keys().size() <= MAX_SLIDES, "한 번에 최대 200개 구절을 만들 수 있습니다.");
        var slides = generator.buildSlides(parsed.keys(), request.lineLimit());
        require(slides.size() <= MAX_SLIDES, "분할된 슬라이드가 200장을 넘습니다. 구절 수를 줄여 주세요.");
        return slides;
    }

    public byte[] preview(Slide slide) throws IOException {
        validate(slide);
        var raster = generator.renderPreview(new SlideContent(slide.title(), slide.body()), style(slide), 960);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(raster.image(), "png", bytes);
        return bytes.toByteArray();
    }

    public byte[] download(Deck request) throws IOException {
        require(request != null && request.slides() != null && !request.slides().isEmpty()
                && request.slides().size() <= MAX_SLIDES, "슬라이드는 1~200장이어야 합니다.");
        request.slides().forEach(this::validate);
        require(request.slides().stream().mapToInt(s -> s.body().length()).sum() <= 100_000, "본문이 너무 깁니다.");
        var contents = request.slides().stream().map(s -> new SlideContent(s.title(), s.body())).toList();
        var index = new java.util.concurrent.atomic.AtomicInteger();
        var bytes = new ByteArrayOutputStream();
        generator.writeToStream(contents, bytes, ignored -> style(request.slides().get(index.getAndIncrement())));
        return bytes.toByteArray();
    }

    private PresentationStyle style(Slide slide) {
        var s = slide.style() == null ? metadata.defaultStyle() : slide.style();
        return new PresentationStyle(s.titleFontSize(), s.bodyFontSize(), s.title().rectangle(), s.body().rectangle());
    }

    private void validate(Slide slide) {
        require(slide != null && slide.title() != null && slide.body() != null, "슬라이드 제목과 본문이 필요합니다.");
        require(slide.title().length() <= 500 && slide.body().length() <= 8000, "슬라이드 내용이 너무 깁니다.");
        if (slide.style() == null) return;
        var s = slide.style();
        require(Double.isFinite(s.titleFontSize()) && s.titleFontSize() >= 8 && s.titleFontSize() <= 120
                && Double.isFinite(s.bodyFontSize()) && s.bodyFontSize() >= 8 && s.bodyFontSize() <= 120,
                "글자 크기는 8~120pt 사이로 입력해 주세요.");
        validateBox(s.title());
        validateBox(s.body());
    }

    private void validateBox(Box b) {
        require(b != null && Double.isFinite(b.x()) && Double.isFinite(b.y())
                && Double.isFinite(b.width()) && Double.isFinite(b.height())
                && b.x() >= 0 && b.y() >= 0 && b.width() >= 1 && b.height() >= 1
                && b.x() + b.width() <= metadata.width() + 0.1
                && b.y() + b.height() <= metadata.height() + 0.1, "텍스트 상자는 슬라이드 안에 배치해 주세요.");
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }

    public record BuildRequest(String reference, int lineLimit) {}
    public record Deck(List<Slide> slides) {}
    public record Slide(String title, String body, Style style) {}
    public record Metadata(List<BibleBook> books, double width, double height, Style defaultStyle, int maxSlides) {}
    public record Box(double x, double y, double width, double height) {
        Rectangle2D rectangle() { return new Rectangle2D.Double(x, y, width, height); }
        static Box from(Rectangle2D r) { return new Box(r.getX(), r.getY(), r.getWidth(), r.getHeight()); }
    }
    public record Style(double titleFontSize, double bodyFontSize, Box title, Box body) {
        static Style from(PresentationStyle s) {
            return new Style(s.titleFontSize(), s.bodyFontSize(), Box.from(s.titleAnchor()), Box.from(s.bodyAnchor()));
        }
    }
}
