package com.hotty.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRewardsDataModel {

    /**
     * Timestamp (en milisegundos desde la época) que indica cuándo estará disponible la próxima recompensa diaria.
     */
    private Long nextDailyRewardTimestamp = 0L;

    /**
     * Recompensa de única vez que se otorga al usuario después de crear su perfil.
     */
    private Boolean waitingFirstReward = true;

    /**
     * Verdadero si el usuario ha sido invitado por otro y tiene una recompensa pendiente por reclamar.
     */
    private Boolean promotionalCodePendingOfUse = false;

    /**
     * Verdadero si el usuario tiene derecho a una recompensa después de una verificación de perfil exitosa.
     */
    private Boolean rewardForVerificationRight = false;

    /**
     * Un indicador general que señala si hay una recompensa pendiente.
     */
    private Boolean waitingReward = false;

    /**
     * Indica si el usuario tiene una suscripción premium.
     */
    private Boolean isPremium = false;

    /**
     * El número total de monedas que tiene el usuario.
     */
    private Integer coins = 0;


    /**
     * El código promocional que este usuario ha utilizado (por ejemplo, de una invitación).
     */
    private String promotionalCodeUsedByUser = "NOT_AVAILABLE";

    /**
     * El número de veces que el enlace de referido del usuario ha sido utilizado con éxito por otros.
     */
    private Integer successfulShares = 0;
}