import {useTranslation} from "react-i18next";
import {
    DataListItem,
    DataListItemRow,
    DataListDragButton,
    DataListItemCells,
    DataListCell,
} from "@patternfly/react-core";

import "./flow-header.css";

type AuthenticationPolicyHeaderProps = {
    isParentPolicy?: boolean
};

export const AuthenticationPolicyHeader = ({isParentPolicy}: AuthenticationPolicyHeaderProps) => {
    const {t} = useTranslation();
    return (
        <DataListItem aria-labelledby="headerName" id="header">
            <DataListItemRow>
                <DataListDragButton
                    className="keycloak__authentication__header-drag-button"
                    aria-label={t("disabled")}
                />
                <DataListItemCells
                    className="keycloak__authentication__header"
                    dataListCells={[
                        <DataListCell key="step" id="headerName">
                            {t("steps")}
                        </DataListCell>,
                        <>
                            {isParentPolicy && (
                                <DataListCell key="enabled">{t("enabled")}</DataListCell>
                            )}
                            {!isParentPolicy && (
                                <DataListCell key="requirement">{t("requirement")}</DataListCell>
                            )}
                        </>,
                        <DataListCell key="config"></DataListCell>,
                    ]}
                />
            </DataListItemRow>
        </DataListItem>
    );
};
