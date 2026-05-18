# Search Filter Matrix

| UI Filter | API Param | SQL Source | Status |
|---|---|---|---|
| Type of Matches: All | none | default query | Implemented |
| Type of Matches: Verified | `verifiedOnly` | `profiles.verification_status='verified'` | Implemented |
| Type of Matches: Viewed | `viewedOnly` | `profile_views` joined through current user's profile | Implemented |
| Type of Matches: Just Joined | `activityOnSite=today` or client date preset | `users.last_login`, `profiles.created_at` supported through activity/date sorting | Partial |
| Type of Matches: Nearby | `nearbyOnly` | viewer/profile `working_city`, `family_city`, `working_state`, `family_state` | Implemented |
| Religion | `religion` | `profiles.religion` | Implemented |
| Community / Caste | `community` or `caste` | `profiles.caste`, `profiles.religion` | Implemented |
| Mother Tongue | `motherTongue` | `profiles.mother_tongue` | Implemented |
| Online Status | `onlineStatus` | `users.last_login` | Implemented |
| Activity on Site | `activityOnSite` | `users.last_login` | Implemented |
| Profile Posted By | `profilePostedBy` | `profiles.profile_created_by` | Implemented |
| Country | `country` | India/abroad inferred from available state fields | Partial |
| State | `state`, `familyState`, `workingState` | `family_details.family_state`, `education_career.working_state` | Implemented |
| City | `city`, `location` | `education_career.working_city` | Implemented |
| Income | `income`, `annualIncome` | `education_career.annual_income` | Implemented |
| Education | `education`, `educationLevel` | `education_career.education_level` | Implemented |
| Employed In | `employedIn`, `employmentType` | `occupation`, `working_city`, `working_state` text match | Implemented |
| Currently Employed | `isEmployed`, `currentlyEmployed` | `education_career.is_employed` | Implemented |
| Occupation | `occupation` | `education_career.occupation` | Implemented |
| Photo | `photoOnly`, `hasPhotoOnly` | `profiles.primary_photo_url` | Implemented |
| Height | `heightMinCm`, `heightMaxCm` | `physical_details.height_cm` | Implemented |
| Age | `ageMin`, `ageMax` | `profiles.dob` | Implemented |
| Marital Status | `maritalStatus` | `profiles.marital_status` | Implemented |
| Horoscope | `horoscope`, `hasHoroscope` | `horoscope_details.rashi`, `nakshatra`, `birth_city` | Implemented |
| Manglik | `manglik` | `horoscope_details.is_manglik` | Implemented |
| Diet | `diet` | `lifestyle_details.diet` | Implemented |

All search responses now return `appliedFilters` so Android can display the server-confirmed filter set.
