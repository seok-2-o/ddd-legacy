package kitchenpos.application;

import kitchenpos.domain.*;
import kitchenpos.infra.PurgomalumClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.Boolean.*;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;
    @Mock
    private MenuGroupRepository menuGroupRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PurgomalumClient purgomalumClient;

    @InjectMocks
    private MenuService menuService;

    private static Stream<BigDecimal> providePriceForNullAndNegative() { // argument source method
        return Stream.of(
                null,
                BigDecimal.valueOf(-1000L)
        );
    }

    @DisplayName("메뉴등록 - 메뉴의 가격은 반드시 0보다 큰 값을 가져야 한다.")
    @MethodSource("providePriceForNullAndNegative")
    @ParameterizedTest
    public void create01(BigDecimal 등록할_메뉴_가격) {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴등록 - 메뉴는 반드시 하나의 메뉴 그룹에 속해야 한다.")
    @Test
    public void create02() {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.empty());
        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(NoSuchElementException.class);
    }


    private static Stream<List<MenuProduct>> provideMenuProductForNullAndEmpty() { // argument source method
        return Stream.of(
                null,
                Collections.emptyList()
        );
    }

    @DisplayName("메뉴등록 - 메뉴는 반드시 하나 이상의 상품(product)을 포함해야 한다.")
    @MethodSource("provideMenuProductForNullAndEmpty")
    @ParameterizedTest
    public void create03(List<MenuProduct> 메뉴에_등록될_상품들) {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));
        given(등록할_메뉴.getMenuProducts()).willReturn(메뉴에_등록될_상품들);
        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴등록 - 메뉴에 포함할 상품은 반드시 존재해야 한다.")
    @Test
    public void create03() {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));

        List<MenuProduct> 등록할_상품들 = mock(List.class);
        int 등록할_상품수 = 2;
        given(등록할_상품들.size()).willReturn(등록할_상품수);
        given(등록할_메뉴.getMenuProducts()).willReturn(등록할_상품들);
        List<Product> 조회된_상품들 = mock(List.class);
        int 조회된_상품수 = 1;
        given(조회된_상품들.size()).willReturn(조회된_상품수);
        given(productRepository.findAllByIdIn(anyList())).willReturn(조회된_상품들);

        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // @TODO 품목 별 수량 0개 등록 가능 -> 1개 이상 등록 가능하도록 개선
    @DisplayName("메뉴등록 - 메뉴는 반드시 하나 이상의 상품(product)을 포함해야 한다. - 품목별 수량은 -1보다 큰 값을 가져야 한다.")
    @Test
    public void create04() {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        long 등록할_상품_품목_수량 = -1l;

        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));

        List<MenuProduct> 등록할_상품들 = spy(ArrayList.class);
        MenuProduct 등록할_상품 = mock(MenuProduct.class);
        given(등록할_상품.getQuantity()).willReturn(등록할_상품_품목_수량);
        given(등록할_상품.getProductId()).willReturn(UUID.randomUUID());
        등록할_상품들.add(등록할_상품);
        given(등록할_메뉴.getMenuProducts()).willReturn(등록할_상품들);

        List<Product> 조회된_상품들 = mock(List.class);
        int 조회된_상품수 = 1;
        given(조회된_상품들.size()).willReturn(조회된_상품수);
        given(productRepository.findAllByIdIn(anyList())).willReturn(조회된_상품들);

        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @DisplayName("메뉴등록 - 메뉴에 가격은 메뉴에 속한 모든 상품의 가격의 합보다 클 수 없다.")
    @Test
    public void create05() {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        long 등록할_상품_품목_수량 = 1l;

        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));

        List<MenuProduct> 등록할_상품들 = spy(ArrayList.class);
        MenuProduct 등록할_상품 = mock(MenuProduct.class);
        given(등록할_상품.getQuantity()).willReturn(등록할_상품_품목_수량);
        given(등록할_상품.getProductId()).willReturn(UUID.randomUUID());
        등록할_상품들.add(등록할_상품);
        given(등록할_메뉴.getMenuProducts()).willReturn(등록할_상품들);

        List<Product> 조회된_상품들 = mock(List.class);
        int 조회된_상품수 = 1;
        given(조회된_상품들.size()).willReturn(조회된_상품수);
        given(productRepository.findAllByIdIn(anyList())).willReturn(조회된_상품들);

        Product 조회된_상품 = mock(Product.class);
        BigDecimal 조회된_상품_가격 = BigDecimal.valueOf(500l);
        given(조회된_상품.getPrice()).willReturn(조회된_상품_가격);
        given(productRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(조회된_상품));

        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(IllegalArgumentException.class);

    }
    // @TODO 메뉴 이름 EmptyString 가능함 -> EmptyString 유효성 검증
    @DisplayName("메뉴등록 -  메뉴는 반드시 이름을 가져야 한다.")
    @Test
    public void create06() {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        long 등록할_상품_품목_수량 = 1l;

        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));

        List<MenuProduct> 등록할_상품들 = spy(ArrayList.class);
        MenuProduct 등록할_상품 = mock(MenuProduct.class);
        given(등록할_상품.getQuantity()).willReturn(등록할_상품_품목_수량);
        given(등록할_상품.getProductId()).willReturn(UUID.randomUUID());
        등록할_상품들.add(등록할_상품);
        given(등록할_메뉴.getMenuProducts()).willReturn(등록할_상품들);

        List<Product> 조회된_상품들 = mock(List.class);
        int 조회된_상품수 = 1;
        given(조회된_상품들.size()).willReturn(조회된_상품수);
        given(productRepository.findAllByIdIn(anyList())).willReturn(조회된_상품들);

        Product 조회된_상품 = mock(Product.class);
        BigDecimal 조회된_상품_가격 = BigDecimal.valueOf(1500l);
        given(조회된_상품.getPrice()).willReturn(조회된_상품_가격);
        given(productRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(조회된_상품));

        given(등록할_메뉴.getName()).willReturn(null);
        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("메뉴등록 -  메뉴의 이름은 비속어를 포함할 수 없다.")
    @Test
    public void create07() {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        long 등록할_상품_품목_수량 = 1l;

        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));

        List<MenuProduct> 등록할_상품들 = spy(ArrayList.class);
        MenuProduct 등록할_상품 = mock(MenuProduct.class);
        given(등록할_상품.getQuantity()).willReturn(등록할_상품_품목_수량);
        given(등록할_상품.getProductId()).willReturn(UUID.randomUUID());
        등록할_상품들.add(등록할_상품);
        given(등록할_메뉴.getMenuProducts()).willReturn(등록할_상품들);

        List<Product> 조회된_상품들 = mock(List.class);
        int 조회된_상품수 = 1;
        given(조회된_상품들.size()).willReturn(조회된_상품수);
        given(productRepository.findAllByIdIn(anyList())).willReturn(조회된_상품들);

        Product 조회된_상품 = mock(Product.class);
        BigDecimal 조회된_상품_가격 = BigDecimal.valueOf(1500l);
        given(조회된_상품.getPrice()).willReturn(조회된_상품_가격);
        given(productRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(조회된_상품));

        String 등록할_메뉴_이름 = "X나 맛있는 미트파이 정식";
        given(등록할_메뉴.getName()).willReturn(등록할_메뉴_이름);
        when(purgomalumClient.containsProfanity(등록할_메뉴_이름))
                .thenReturn(TRUE);
        //when & then
        assertThatThrownBy(() -> menuService.create(등록할_메뉴))
                .isInstanceOf(IllegalArgumentException.class);
    }



    @DisplayName("메뉴등록 -  메뉴를 등록 할 수 있다.")
    @Test
    public void create08() {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        long 등록할_상품_품목_수량 = 1l;

        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));

        List<MenuProduct> 등록할_상품들 = spy(ArrayList.class);
        MenuProduct 등록할_상품 = mock(MenuProduct.class);
        given(등록할_상품.getQuantity()).willReturn(등록할_상품_품목_수량);
        given(등록할_상품.getProductId()).willReturn(UUID.randomUUID());
        등록할_상품들.add(등록할_상품);
        given(등록할_메뉴.getMenuProducts()).willReturn(등록할_상품들);

        List<Product> 조회된_상품들 = mock(List.class);
        int 조회된_상품수 = 1;
        given(조회된_상품들.size()).willReturn(조회된_상품수);
        given(productRepository.findAllByIdIn(anyList())).willReturn(조회된_상품들);

        Product 조회된_상품 = mock(Product.class);
        BigDecimal 조회된_상품_가격 = BigDecimal.valueOf(1500l);
        given(조회된_상품.getPrice()).willReturn(조회된_상품_가격);
        given(productRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(조회된_상품));


        String 등록할_메뉴_이름 = "맛있는 미트파이 정식";
        given(등록할_메뉴.getName()).willReturn(등록할_메뉴_이름);
        when(purgomalumClient.containsProfanity(등록할_메뉴_이름))
                .thenReturn(FALSE);

        //when & then
        menuService.create(등록할_메뉴);

        // then
        verify(menuRepository).save(any(Menu.class));
    }

    private static Stream<Arguments> provideDisplayFlagForTrueAndFalse() { // argument source method
        return Stream.of(
                Arguments.of(TRUE, TRUE),
                Arguments.of(FALSE, FALSE)
        );
    }

    @DisplayName("메뉴등록 -  메뉴는 노출 여부를 가진다.")
    @MethodSource("provideDisplayFlagForTrueAndFalse")
    @ParameterizedTest
    public void create09(Boolean 등록할_노출_여부, Boolean 등록된_노출_여부) {
        //given
        Menu 등록할_메뉴 = mock(Menu.class);
        BigDecimal 등록할_메뉴_가격 = BigDecimal.valueOf(1000l);
        long 등록할_상품_품목_수량 = 1l;

        given(등록할_메뉴.getPrice()).willReturn(등록할_메뉴_가격);
        given(등록할_메뉴.getMenuGroupId()).willReturn(UUID.randomUUID());
        given(menuGroupRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(mock(MenuGroup.class)));

        List<MenuProduct> 등록할_상품들 = spy(ArrayList.class);
        MenuProduct 등록할_상품 = mock(MenuProduct.class);
        given(등록할_상품.getQuantity()).willReturn(등록할_상품_품목_수량);
        given(등록할_상품.getProductId()).willReturn(UUID.randomUUID());
        등록할_상품들.add(등록할_상품);
        given(등록할_메뉴.getMenuProducts()).willReturn(등록할_상품들);

        List<Product> 조회된_상품들 = mock(List.class);
        int 조회된_상품수 = 1;
        given(조회된_상품들.size()).willReturn(조회된_상품수);
        given(productRepository.findAllByIdIn(anyList())).willReturn(조회된_상품들);

        Product 조회된_상품 = mock(Product.class);
        BigDecimal 조회된_상품_가격 = BigDecimal.valueOf(1500l);
        given(조회된_상품.getPrice()).willReturn(조회된_상품_가격);
        given(productRepository.findById(any(UUID.class)))
                .willReturn(Optional.of(조회된_상품));


        String 등록할_메뉴_이름 = "맛있는 미트파이 정식";
        given(등록할_메뉴.getName()).willReturn(등록할_메뉴_이름);
        when(purgomalumClient.containsProfanity(등록할_메뉴_이름))
                .thenReturn(FALSE);

        given(등록할_메뉴.isDisplayed()).willReturn(등록할_노출_여부);
        when(menuRepository.save(any(Menu.class))).thenAnswer(params -> params.getArgument(0)); // by pass
        //when & then
        Menu menu = menuService.create(등록할_메뉴);

        // then
        assertThat(menu.isDisplayed()).isEqualTo(등록된_노출_여부);
    }


}