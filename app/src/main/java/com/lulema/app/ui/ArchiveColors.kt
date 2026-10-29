package com.lulema.app.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Set every container role so sheets, dialogs, fields and selected controls stay neutral too.
internal val ArchiveLightColors=lightColorScheme(
 primary=Color(0xFF111111),onPrimary=Color.White,primaryContainer=Color(0xFFE5E5E7),onPrimaryContainer=Color(0xFF111111),
 secondary=Color(0xFF444448),onSecondary=Color.White,secondaryContainer=Color(0xFFE8E8EB),onSecondaryContainer=Color(0xFF111111),
 tertiary=Color(0xFF55555A),onTertiary=Color.White,tertiaryContainer=Color(0xFFEEEEF0),onTertiaryContainer=Color(0xFF111111),
 background=Color.White,onBackground=Color(0xFF111111),surface=Color.White,onSurface=Color(0xFF111111),
 surfaceVariant=Color(0xFFF0F0F2),onSurfaceVariant=Color(0xFF636369),surfaceTint=Color.Transparent,
 outline=Color(0xFF77777D),outlineVariant=Color(0xFFD4D4D8),inverseSurface=Color(0xFF222225),inverseOnSurface=Color.White,inversePrimary=Color.White,
 surfaceDim=Color(0xFFE6E6E9),surfaceBright=Color.White,surfaceContainerLowest=Color.White,
 surfaceContainerLow=Color(0xFFFAFAFB),surfaceContainer=Color(0xFFF5F5F7),surfaceContainerHigh=Color(0xFFEEEEF0),surfaceContainerHighest=Color(0xFFE5E5E7)
)
internal val ArchiveDarkColors=darkColorScheme(
 primary=Color.White,onPrimary=Color.Black,primaryContainer=Color(0xFF333336),onPrimaryContainer=Color.White,
 secondary=Color(0xFFCCCCD0),onSecondary=Color.Black,secondaryContainer=Color(0xFF333336),onSecondaryContainer=Color.White,
 tertiary=Color(0xFFBBBBBF),onTertiary=Color.Black,tertiaryContainer=Color(0xFF2C2C30),onTertiaryContainer=Color.White,
 background=Color.Black,onBackground=Color.White,surface=Color.Black,onSurface=Color.White,
 surfaceVariant=Color(0xFF222225),onSurfaceVariant=Color(0xFFB0B0B7),surfaceTint=Color.Transparent,
 outline=Color(0xFF909098),outlineVariant=Color(0xFF444448),inverseSurface=Color(0xFFE5E5E7),inverseOnSurface=Color(0xFF111111),inversePrimary=Color(0xFF111111),
 surfaceDim=Color.Black,surfaceBright=Color(0xFF29292D),surfaceContainerLowest=Color.Black,
 surfaceContainerLow=Color(0xFF111113),surfaceContainer=Color(0xFF1C1C1E),surfaceContainerHigh=Color(0xFF252528),surfaceContainerHighest=Color(0xFF333336)
)
