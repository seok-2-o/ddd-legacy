package kitchenpos.application;

import kitchenpos.domain.*;
import kitchenpos.infra.PurgomalumClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private PurgomalumClient purgomalumClient;

    @InjectMocks
    private ProductService productService;

    private static Stream<BigDecimal> providePriceForNullAndNegative() { // argument source method
        return Stream.of(
                null,
                BigDecimal.valueOf(-1000L)
        );
    }

    @DisplayName("상품등록 - 상품의 가격은 반드시 0보다 큰 값을 가져야 한다.")
    @MethodSource("providePriceForNullAndNegative")
    @ParameterizedTest
    public void create01(BigDecimal input) {
        //given
        Product product = new Product();
        product.setPrice(input);
        //when & then
        assertThatThrownBy(() -> productService.create(product))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품등록 - 상품은 반드시 이름을 가진다.")
    @Test
    public void create02() {
        //given
        Product product = new Product();
        product.setPrice(BigDecimal.valueOf(1000l));
        //when & then
        assertThatThrownBy(() -> productService.create(product))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품등록 - 상품의 이름은 비속어를 포함할 수 없다.")
    @Test
    public void create03() {
        //given
        Product product = new Product();
        product.setPrice(BigDecimal.valueOf(1000l));
        String slang = "X나 맛없는 미트파이";
        product.setName(slang);
        when(purgomalumClient.containsProfanity(slang))
                .thenReturn(Boolean.TRUE);
        //when & then
        assertThatThrownBy(() -> productService.create(product))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품등록 - 상품을 등록할 수 있다.")
    @Test
    public void create04() {
        //given
        Product request = new Product();
        request.setPrice(BigDecimal.valueOf(1000l));
        String name = "맛있는 미트파이";
        request.setName(name);
        when(purgomalumClient.containsProfanity(name))
                .thenReturn(Boolean.FALSE);
        //when
        productService.create(request);

        // & then
        verify(productRepository).save(any(Product.class));
    }

    @DisplayName("상품 가격 수정 - 상품의 가격은 반드시 0보다 큰 값을 가져야 한다.")
    @MethodSource("providePriceForNullAndNegative")
    @ParameterizedTest
    public void changePrice01(BigDecimal 변경할_상품_가격) {
        //given
        Product 변경할_상품 = new Product();
        변경할_상품.setPrice(변경할_상품_가격);
        //when & then
        assertThatThrownBy(() -> productService.changePrice(UUID.randomUUID(), 변경할_상품))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 가격 수정 - 가격을 변경하는 상품을 포함하는 메뉴의 가격보다 메뉴에 포함한 상품의 가격이 커지는 경우 메뉴를 진열하지 않는다.")
    @Test
    public void changePrice02() {
        //given
        BigDecimal 변경할_상품_가격 = BigDecimal.valueOf(2700l);
        BigDecimal 기존_상품_가격 = BigDecimal.valueOf(3000l);
        BigDecimal 계속_공개될_메뉴_가격 = BigDecimal.valueOf(2500l);
        BigDecimal 비공개될_메뉴_가격 = BigDecimal.valueOf(2800l);

        Product 변경할_상품 = new Product();
        변경할_상품.setPrice(변경할_상품_가격);

        Product 저장된_상품 = spy(Product.class);
        저장된_상품.setPrice(기존_상품_가격);
        given(productRepository.findById(any(UUID.class))).willReturn(Optional.of(저장된_상품));

        MenuProduct 저장된_메뉴_상품 = mock(MenuProduct.class);
        given(저장된_메뉴_상품.getProduct()).willReturn(저장된_상품);
        given(저장된_메뉴_상품.getQuantity()).willReturn(1l);

        Menu 계속_공개될_메뉴 = spy(Menu.class);
        계속_공개될_메뉴.setDisplayed(true);
        given(계속_공개될_메뉴.getMenuProducts()).willReturn(new ArrayList<>(Arrays.asList(저장된_메뉴_상품)));
        given(계속_공개될_메뉴.getPrice()).willReturn(계속_공개될_메뉴_가격);

        Menu 비공개될_메뉴 = spy(Menu.class);
        비공개될_메뉴.setDisplayed(true);
        given(비공개될_메뉴.getMenuProducts()).willReturn(new ArrayList<>(Arrays.asList(저장된_메뉴_상품)));
        given(비공개될_메뉴.getPrice()).willReturn(비공개될_메뉴_가격);

        given(menuRepository.findAllByProductId(any(UUID.class)))
                .willReturn(new ArrayList<>(Arrays.asList(계속_공개될_메뉴, 비공개될_메뉴)));


        //when
        productService.changePrice(UUID.randomUUID(), 변경할_상품);

        //then
        assertThat(계속_공개될_메뉴.isDisplayed()).isTrue();
        assertThat(비공개될_메뉴.isDisplayed()).isFalse();
    }


    @DisplayName("상품 가격 수정 - 상품의 가격을 수정할 수 있다.")
    @Test
    public void changePrice03() {
        //given
        BigDecimal 변경할_상품_가격 = BigDecimal.valueOf(2700l);
        BigDecimal 기존_상품_가격 = BigDecimal.valueOf(3000l);
        BigDecimal 계속_공개될_메뉴_가격 = BigDecimal.valueOf(2500l);

        Product 변경할_상품 = new Product();
        변경할_상품.setPrice(변경할_상품_가격);
        Product 저장된_상품 = spy(Product.class);
        저장된_상품.setPrice(기존_상품_가격);
        given(productRepository.findById(any(UUID.class))).willReturn(Optional.of(저장된_상품));

        MenuProduct 저장된_메뉴_상품 = mock(MenuProduct.class);
        given(저장된_메뉴_상품.getProduct()).willReturn(저장된_상품);
        given(저장된_메뉴_상품.getQuantity()).willReturn(1l);

        Menu 계속_공개될_메뉴 = spy(Menu.class);
        계속_공개될_메뉴.setDisplayed(true);
        given(계속_공개될_메뉴.getMenuProducts()).willReturn(new ArrayList<>(Arrays.asList(저장된_메뉴_상품)));
        given(계속_공개될_메뉴.getPrice()).willReturn(계속_공개될_메뉴_가격);

        given(menuRepository.findAllByProductId(any(UUID.class)))
                .willReturn(new ArrayList<>(Arrays.asList(계속_공개될_메뉴)));
        //when
        Product 변경된_상품 = productService.changePrice(UUID.randomUUID(), 변경할_상품);

        //then
        assertThat(변경된_상품.getPrice()).isEqualTo(변경할_상품_가격);
    }

    @DisplayName("상품 조회 - 등록된 모든 상품을 조회할 수 있다.")
    @Test
    void findAll() {
        // given & when
        productService.findAll();
        //then
        verify(productRepository).findAll();
    }
}