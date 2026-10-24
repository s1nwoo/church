package com.banghwa.bibleppt.ppt;

import com.banghwa.bibleppt.config.AppConfig;
import com.banghwa.bibleppt.data.BibleRepository;
import com.banghwa.bibleppt.model.PresentationStyle;
import com.banghwa.bibleppt.model.SlideContent;
import com.banghwa.bibleppt.model.VerseReference;
import org.apache.poi.common.usermodel.fonts.FontGroup;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 구절 키 목록으로부터 슬라이드 내용을 만들고, 템플릿 기반 PowerPoint 파일을 생성한다.
 *
 * <p>설계 핵심: <b>{@link #buildSlides}</b>가 "슬라이드 내용(텍스트) 계산"을 담당하고,
 * <b>{@link #renderPreview}</b>가 "실제 모습 렌더링", <b>{@link #writeToStream}</b>가
 * "파일 쓰기"를 담당한다. 셋 다 같은 {@link #createSlide}로 슬라이드를 만들기 때문에,
 * 미리보기에서 본 모습과 최종 저장된 PPT가 항상 일치한다.</p>
 */
public final class PptGenerator {

    /** 텍스트 상자 안쪽 여백(pt). 템플릿 도형에 일괄 적용한다. */
    private static final double TEXT_INSET = 5.0;

    private final AppConfig config;
    private final BibleRepository repository;

    public PptGenerator(AppConfig config, BibleRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    /**
     * 구절 키 목록을 슬라이드 내용 목록으로 변환한다. (파일을 만들지 않는다 → 미리보기에 사용)
     *
     * <p>절마다 최소 한 장을 보장한다 — 길면 그 절만 줄 수 제한에 따라 여러 장으로 나뉜다.
     * 각 절의 본문 맨 앞에는 절 번호를 붙인다(합쳤을 때뿐 아니라 평소에도 어느 절인지 바로
     * 보이도록). 한 절이 길어서 여러 장으로 나뉘는 경우, 번호는 그 절이 시작하는 첫 장에만
     * 붙고 이어지는 장에는 반복하지 않는다. 여러 절을 한 슬라이드에 모으고 싶다면 화면의
     * 슬라이드 목록에서 직접 "합치기"로 묶으면 된다(사용자가 원하는 조합을 그대로 반영할 수 있다).</p>
     *
     * @param verseKeys 존재가 검증된 구절 키 목록
     * @param lineLimit 슬라이드당 최대 줄 수 (0 이하이면 제한 없음)
     * @return 슬라이드 내용(제목+본문) 목록
     */
    public List<SlideContent> buildSlides(List<String> verseKeys, int lineLimit) {
        List<SlideContent> slides = new ArrayList<>();
        for (String key : verseKeys) {
            String verseText = repository.getVerse(key);
            if (verseText == null) {
                continue; // 검증을 거쳤다면 발생하지 않지만 방어적으로 건너뛴다.
            }
            String verseNumberLabel = VerseReference.parse(key)
                    .map(ref -> String.valueOf(ref.verse()))
                    .orElse("");

            List<String> chunks = SlideChunker.chunk(verseText, lineLimit, config.getWrapWidth());
            for (int i = 0; i < chunks.size(); i++) {
                boolean isFirstChunkOfVerse = i == 0;
                String body = (isFirstChunkOfVerse && !verseNumberLabel.isEmpty())
                        ? verseNumberLabel + " " + chunks.get(i)
                        : chunks.get(i);
                slides.add(new SlideContent(key, body));
            }
        }
        return slides;
    }

    /**
     * 템플릿에서 제목·본문 도형의 기본 위치와 글꼴 크기를 읽어 기본 스타일을 만든다.
     * 세션 시작 시 한 번 호출해 "초기화" 버튼이 되돌아갈 기준값으로 쓴다.
     *
     * <p>글꼴 크기는 우선 템플릿 도형에 이미 지정된 값을 그대로 읽어온다(제목·본문이 서로
     * 다른 크기일 수 있으므로). 도형에 크기가 지정돼 있지 않으면 설정({@code slide.fontSize})의
     * 값을 기본값으로 쓴다.</p>
     *
     * @return 템플릿 기반 기본 스타일
     * @throws IOException 템플릿을 읽지 못한 경우
     * @throws IllegalStateException 템플릿에 슬라이드가 없거나 제목/본문 도형을 찾지 못한 경우
     */
    public PresentationStyle readDefaultStyle() throws IOException {
        try (InputStream in = config.openTemplate();
             XMLSlideShow template = new XMLSlideShow(in)) {

            XSLFSlide slide = firstSlideOrThrow(template);
            Rectangle2D titleAnchor = null;
            Rectangle2D bodyAnchor = null;
            Double titleFontSize = null;
            Double bodyFontSize = null;

            for (XSLFShape shape : slide.getShapes()) {
                if (!(shape instanceof XSLFTextShape textShape)) {
                    continue;
                }
                String text = textShape.getText();
                if (text == null) {
                    continue;
                }
                if (text.contains(config.getTitlePlaceholder())) {
                    titleAnchor = textShape.getAnchor();
                    titleFontSize = firstExplicitFontSize(textShape);
                } else if (text.contains(config.getBodyPlaceholder())) {
                    bodyAnchor = textShape.getAnchor();
                    bodyFontSize = firstExplicitFontSize(textShape);
                }
            }

            if (titleAnchor == null || bodyAnchor == null) {
                throw new IllegalStateException(
                        "템플릿에서 " + config.getTitlePlaceholder() + "/" + config.getBodyPlaceholder()
                                + " 도형을 찾을 수 없습니다.");
            }
            double titleSize = titleFontSize != null ? titleFontSize : config.getFontSize();
            double bodySize = bodyFontSize != null ? bodyFontSize : config.getFontSize();
            return new PresentationStyle(titleSize, bodySize, titleAnchor, bodyAnchor);
        }
    }

    /** 도형 안 첫 텍스트 런에 명시적으로 지정된 글꼴 크기를 찾는다. 없으면 {@code null}. */
    private Double firstExplicitFontSize(XSLFTextShape textShape) {
        for (XSLFTextParagraph para : textShape.getTextParagraphs()) {
            for (XSLFTextRun run : para.getTextRuns()) {
                Double size = run.getFontSize();
                if (size != null) {
                    return size;
                }
            }
        }
        return null;
    }

    /**
     * 슬라이드 한 장을 실제 PPT와 동일하게 래스터 이미지로 렌더링한다. (미리보기 전용, 파일을 만들지 않음)
     *
     * @param slide      렌더링할 슬라이드 내용
     * @param style      적용할 글꼴 크기·위치
     * @param pixelWidth 렌더링할 이미지의 가로 픽셀 크기 (세로는 슬라이드 비율에 맞춰 자동 계산)
     * @return 렌더링 결과(이미지 + 픽셀↔포인트 환산에 필요한 슬라이드 크기)
     * @throws IOException 템플릿을 읽지 못한 경우
     */
    public SlideRaster renderPreview(SlideContent slide, PresentationStyle style, int pixelWidth) throws IOException {
        try (InputStream in = config.openTemplate();
             XMLSlideShow template = new XMLSlideShow(in);
             XMLSlideShow single = new XMLSlideShow()) {

            XSLFSlide templateSlide = firstSlideOrThrow(template);
            single.setPageSize(template.getPageSize());
            createSlide(single, templateSlide, slide, style);

            Dimension pageSize = single.getPageSize(); // 단위: 포인트(pt)
            double scale = pixelWidth / (double) pageSize.width;
            int pixelHeight = Math.max(1, (int) Math.round(pageSize.height * scale));

            BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.scale(scale, scale);
            single.getSlides().get(0).draw(graphics);
            graphics.dispose();

            return new SlideRaster(image, pageSize.getWidth(), pageSize.getHeight());
        }
    }

    /**
     * 슬라이드 내용을 템플릿에 채워 PowerPoint 파일로 저장한다.
     *
     * <p>스타일은 슬라이드마다 다를 수 있다 — {@code styleResolver}가 각 슬라이드에 적용할
     * 스타일을 결정한다. 세션 전체에 같은 스타일을 쓰려면 {@code slide -> globalStyle}처럼
     * 항상 같은 값을 돌려주는 함수를 넘기면 되고, 특정 슬라이드만 다른 스타일을 쓰려면
     * 그 슬라이드일 때만 다른 값을 돌려주면 된다. (개별 슬라이드 오버라이드는 호출자가 관리한다)</p>
     *
     * @param slides        {@link #buildSlides}로 만든 슬라이드 목록
     * @param outputStream  HTTP 응답으로 전달할 PPT 출력 스트림
     * @param styleResolver 슬라이드마다 적용할 글꼴 크기·위치를 결정하는 함수
     *                      (미리보기에서 계산한 값과 같은 함수를 쓰면 결과가 일치한다)
     * @throws IOException 템플릿 로딩 또는 파일 쓰기 실패 시
     * @throws IllegalStateException 템플릿에 슬라이드가 하나도 없을 때
     */
    public void writeToStream(List<SlideContent> slides, java.io.OutputStream outputStream,
                            Function<SlideContent, PresentationStyle> styleResolver) throws IOException {
        // try-with-resources로 템플릿·출력·결과물 스트림을 모두 안전하게 닫는다.
        try (InputStream templateStream = config.openTemplate();
             XMLSlideShow template = new XMLSlideShow(templateStream);
             XMLSlideShow output = new XMLSlideShow()) {

            XSLFSlide templateSlide = firstSlideOrThrow(template);

            output.setPageSize(template.getPageSize());
            for (SlideContent slide : slides) {
                createSlide(output, templateSlide, slide, styleResolver.apply(slide));
            }

            output.write(outputStream);
        }
    }

    private XSLFSlide firstSlideOrThrow(XMLSlideShow ppt) {
        if (ppt.getSlides().isEmpty()) {
            throw new IllegalStateException("템플릿 PPT에 슬라이드가 없습니다: " + config.getTemplatePath());
        }
        return ppt.getSlides().get(0);
    }

    /** 텍스트 상자가 맡은 역할. 원본 자리표시자 문자열을 기준으로 판별한다. */
    private enum PlaceholderRole { TITLE, BODY, OTHER }

    /** 템플릿 슬라이드를 복제하고, 자리표시자 치환 · 글꼴 · 위치를 스타일에 맞게 적용한 새 슬라이드를 만든다. */
    private void createSlide(XMLSlideShow output, XSLFSlide templateSlide, SlideContent content, PresentationStyle style) {
        XSLFSlide newSlide = output.createSlide();
        newSlide.importContent(templateSlide);

        newSlide.getShapes().forEach(shape -> {
            if (!(shape instanceof XSLFTextShape textShape)) {
                return;
            }
            applyTextBoxInsets(textShape);
            PlaceholderRole role = replacePlaceholders(textShape, content);
            // 한글 폰트는 도형 종류와 상관없이 항상 명시한다 (테마 치환에 의존하면 미리보기에서 깨짐).
            applyFontFamily(textShape);
            switch (role) {
                case TITLE -> applyFontSize(textShape, style.titleFontSize());
                case BODY -> applyFontSize(textShape, style.bodyFontSize());
                case OTHER -> { /* 제목/본문이 아닌 도형은 템플릿의 크기를 그대로 유지한다 */ }
            }
            applyAnchorOverride(textShape, role, style);
        });
    }

    /** 텍스트 상자의 자동 줄바꿈과 안쪽 여백을 설정한다. */
    private void applyTextBoxInsets(XSLFTextShape textShape) {
        textShape.setWordWrap(true);
        textShape.setLeftInset(TEXT_INSET);
        textShape.setRightInset(TEXT_INSET);
        textShape.setTopInset(TEXT_INSET);
        textShape.setBottomInset(TEXT_INSET);
    }

    /**
     * [TITLE]·[BODY] 자리표시자를 실제 제목/본문으로 바꾸고, 원래 어떤 역할이었는지 돌려준다.
     * (자리표시자 문자열은 설정에서 온다)
     */
    private PlaceholderRole replacePlaceholders(XSLFTextShape textShape, SlideContent content) {
        String original = textShape.getText();
        String safeOriginal = original == null ? "" : original;

        PlaceholderRole role = PlaceholderRole.OTHER;
        if (safeOriginal.contains(config.getTitlePlaceholder())) {
            role = PlaceholderRole.TITLE;
        } else if (safeOriginal.contains(config.getBodyPlaceholder())) {
            role = PlaceholderRole.BODY;
        }

        String replaced = safeOriginal
                .replace(config.getTitlePlaceholder(), content.title())
                .replace(config.getBodyPlaceholder(), content.body());
        textShape.setText(replaced);
        return role;
    }

    /** 제목/본문 도형이면 스타일에 지정된 위치로 옮긴다. 그 외 도형은 템플릿 위치를 그대로 유지한다. */
    private void applyAnchorOverride(XSLFTextShape textShape, PlaceholderRole role, PresentationStyle style) {
        switch (role) {
            case TITLE -> textShape.setAnchor(style.titleAnchor());
            case BODY -> textShape.setAnchor(style.bodyAnchor());
            case OTHER -> { /* 그대로 둔다 */ }
        }
    }

    /**
     * 글꼴을 라틴·동아시아 문자 그룹 모두에 명시적으로 지정한다.
     *
     * <p>템플릿이 테마의 "한글 전용 폰트" 치환에만 의존하고 도형에는 폰트를 못 박아두지 않은
     * 경우, PowerPoint는 알아서 치환해 보여주지만 POI의 렌더링 엔진(미리보기)은 이 치환을
     * 하지 않아 한글이 네모(□)로 보인다. 명시적으로 지정하면 미리보기와 실제 PowerPoint
     * 모두 동일하게 보인다.</p>
     */
    private void applyFontFamily(XSLFTextShape textShape) {
        String fontFamily = config.getFontFamily();
        for (XSLFTextParagraph para : textShape.getTextParagraphs()) {
            for (XSLFTextRun run : para.getTextRuns()) {
                run.setFontFamily(fontFamily, FontGroup.LATIN);
                run.setFontFamily(fontFamily, FontGroup.EAST_ASIAN);
            }
        }
    }

    /** 지정된 글꼴 크기를 모든 텍스트 런에 적용한다. */
    private void applyFontSize(XSLFTextShape textShape, double fontSize) {
        for (XSLFTextParagraph para : textShape.getTextParagraphs()) {
            for (XSLFTextRun run : para.getTextRuns()) {
                run.setFontSize(fontSize);
            }
        }
    }

    /**
     * {@link #renderPreview}의 결과. AWT 이미지와, 화면 좌표(픽셀) ↔ PPT 좌표(포인트) 환산에
     * 필요한 슬라이드 크기(포인트)를 함께 담는다. (javafx 의존을 피하기 위해 AWT 타입만 사용 —
     * javafx Image로의 변환은 UI 계층이 담당한다)
     *
     * @param image           렌더링된 래스터 이미지
     * @param pageWidthPoints 슬라이드 가로 크기(포인트)
     * @param pageHeightPoints 슬라이드 세로 크기(포인트)
     */
    public record SlideRaster(BufferedImage image, double pageWidthPoints, double pageHeightPoints) {
    }
}
