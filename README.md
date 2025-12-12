Summapp Android alkalmazás

Célja: rendeléskövető alkamazás (elsődlegesen a Szegedi Nagybani Piac termelőinek).
Az alapértelmezett dátum jelenleg a Szegedi Nagybani Piac igényeihez igazodik, de változtatható a dátum rendelés feltöltésnél:
de az alapértelmezett jelenleg nem változtatható.
A termékeket a felhasználó tudja létrehozni, így bármilyen kategóriában megfelelő lehet az alkalmazás.

Az adatok Firebase Realtime database-be kerülnek feltöltésre.

Az alkalmazás használata bejelentkezéshez kötött - ha nincs elmentve az alkalmazásban korábban bejelentkezett felhasználó:
Az alkalmazás a bejelentkezés felületet mutatja (google bejelentkezés - Firebase Credential manager; bejelentkezés email címmel
vagy regisztráció) - credential manager - a készüléken található Google fiókokat mutatja.
Ha van elmentve belépett felhasználó - automatikusan a privát/csoport nézetválasztó jelenik meg.

A termékek spinnerből választhatóak a rendeléseknél.
Külön oldalon lehet terméket feltölteni a spinnerbe, illetve a rendelés módosításánál is hozzáadható új terméknév.
Az elmentett rendelések alapján a partnerek/vevők listája is megjelenik.
A "Vevők" oldalon az összes rendelés megtekinthető, dátum és/vagy partner szerint szűrhető, törölhető, szerkeszthető.

A rendeléseket lehet rendezni Vevő, Termék, db, Dátum alapján - "Vevők"-nél és "Rendelések" menünél is.
Új terméket az "Új termék"-nél lehet hozzáadni, törölni.
Jegyzeteket is lehet elmenteni, ez is Realtime Database-be töltődik.

Privát/csoport nézet:
Célja: ha több felhasználó szeretné ugyanazokat a rendeléseket, adatokat, jegyzeteket kezelni, csoportos nézetbe lépve megtehető.
Új csoportot bármely bejelentkezett felhasználó létre tud hozni.
Csoport létrehozása csak új azonosító megadásával történik, egyéb azonosítás, feltétel nincs.
(az azonosítót viszont vizsgálja: létezik-e már, illetve egyéb paraméterek)

A csoport létrehozója lesz az admin. Az admin tud csak tagot hozzáadni az adott csoporthoz - email cím alapján.
(a beírt email cím felkerül a tagok listájára, melyeket vizsgál az alkalmazás a csoportba lépés szándékakor)
A felvett tagoknál a tagfelvételi opció nem jelenik meg, csak az adott csoporthoz tartozó adminnál látható a menüelem.

Belépés meglévő csoportba:
A bejelentkezett felhasználó beírja a csoport azonosítóját, amelyhez csatlakozni szeretne.
Ha van ilyen csoport és a felhasználó az adott email címmel hozzátartozik member vagy admin szereppel a csoporthoz - beléphet.
Innentől látja a csoporthoz tartozó adatokat az adatbázisból megjelenítve, hozzáadhat, törölhet és szerkeszthet.

Csoport nézetben a jegyzeteknél látható, ki adta hozzá a jegyzetet (email cím alapján név vagy email cím, amennyiben nincs név).
Addig ebben a csoportban marad alapértelmezetten belépve, amíg csoportot (vagy felhasználót) nem vált, ki nem jelentkezik.

Az alkalmazás a főbb nézeteknél egyértelműen írja, hogy csoport vagy privát nézet van éppen - nehezebb eltéveszteni.
A navigációs menüben csoport nézet esetén írja a belépett csoport azonosítóját is.



