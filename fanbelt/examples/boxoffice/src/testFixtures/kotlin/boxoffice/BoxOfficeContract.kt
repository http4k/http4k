package boxoffice

import boxoffice.domain.ShowTitle

val SHOW = ShowTitle.of("Midnight Owls")

interface BoxOfficeContract :
    SeatingContract,
    CheckoutContract
