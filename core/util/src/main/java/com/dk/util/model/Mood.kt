package com.dk.util.model

import androidx.compose.ui.graphics.Color
import com.dk.ui.theme.AngryColor
import com.dk.ui.theme.AwfulColor
import com.dk.ui.theme.BoredColor
import com.dk.ui.theme.CalmColor
import com.dk.ui.theme.DepressedColor
import com.dk.ui.theme.DisappointedColor
import com.dk.ui.theme.HappyColor
import com.dk.ui.theme.HumorousColor
import com.dk.ui.theme.LonelyColor
import com.dk.ui.theme.MysteriousColor
import com.dk.ui.theme.NeutralColor
import com.dk.ui.theme.RomanticColor
import com.dk.ui.theme.ShamefulColor
import com.dk.ui.theme.SurprisedColor
import com.dk.ui.theme.SuspiciousColor
import com.dk.ui.theme.TenseColor
import com.dk.util.R

enum class Mood(
    val icon: Int,
    val contentColour: Color,
    val containerColour: Color
) {
    Neutral(
        icon = R.drawable.neutral,
        contentColour = Color.Black,
        containerColour = NeutralColor
    ),
    Happy(
        icon = R.drawable.happy,
        contentColour = Color.Black,
        containerColour = HappyColor
    ),
    Angry(
        icon = R.drawable.angry,
        contentColour = Color.White,
        containerColour = AngryColor
    ),
    Bored(
        icon = R.drawable.bored,
        contentColour = Color.Black,
        containerColour = BoredColor
    ),
    Calm(
        icon = R.drawable.calm,
        contentColour = Color.Black,
        containerColour = CalmColor
    ),
    Depressed(
        icon = R.drawable.depressed,
        contentColour = Color.Black,
        containerColour = DepressedColor
    ),
    Disappointed(
        icon = R.drawable.disappointed,
        contentColour = Color.White,
        containerColour = DisappointedColor
    ),
    Humorous(
        icon = R.drawable.humorous,
        contentColour = Color.Black,
        containerColour = HumorousColor
    ),
    Lonely(
        icon = R.drawable.lonely,
        contentColour = Color.White,
        containerColour = LonelyColor
    ),
    Mysterious(
        icon = R.drawable.mysterious,
        contentColour = Color.Black,
        containerColour = MysteriousColor
    ),
    Romantic(
        icon = R.drawable.romantic,
        contentColour = Color.White,
        containerColour = RomanticColor
    ),
    Shameful(
        icon = R.drawable.shameful,
        contentColour = Color.White,
        containerColour = ShamefulColor
    ),
    Awful(
        icon = R.drawable.awful,
        contentColour = Color.Black,
        containerColour = AwfulColor
    ),
    Surprised(
        icon = R.drawable.surprised,
        contentColour = Color.Black,
        containerColour = SurprisedColor
    ),
    Suspicious(
        icon = R.drawable.suspicious,
        contentColour = Color.Black,
        containerColour = SuspiciousColor
    ),
    Tense(
        icon = R.drawable.tense,
        contentColour = Color.Black,
        containerColour = TenseColor
    )
}