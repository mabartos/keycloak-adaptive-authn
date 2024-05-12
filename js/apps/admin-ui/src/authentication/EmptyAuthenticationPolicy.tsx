import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
    Button,
    Flex,
    FlexItem,
    Title,
    TitleSizes,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";

import "./empty-execution-state.css";
import {AddSubPolicyModal, Policy} from "./components/modals/AddSubPolicyModal";

type EmptyAuthenticationPolicyProps = {
    policy: AuthenticationFlowRepresentation;
    onAddSubPolicy: (flow: Policy) => void;
};

export const EmptyAuthenticationPolicy = ({
                                        policy,
                                        onAddSubPolicy,
                                    }: EmptyAuthenticationPolicyProps) => {
    const section = "addPolicy";
    const { t } = useTranslation();
    const [show, setShow] = useState(false);

    return (
        <>
            {show && (
                <AddSubPolicyModal
                    name={policy.alias!}
                    onCancel={() => setShow(false)}
                    onConfirm={(newFlow) => {
                        onAddSubPolicy(newFlow);
                        setShow(false);
                    }}
                />
            )}
            <ListEmptyState
                message={t("emptyAuthenticationPolicy")}
                instructions={t("emptyAuthenticationPolicyInstructions")}
            />

            <div className="keycloak__empty-execution-state__block">
                    <Flex key={section} className="keycloak__empty-execution-state__help">
                        <FlexItem flex={{ default: "flex_1" }}>
                            <Title headingLevel="h2" size={TitleSizes.md}>
                                {t(`${section}Title`)}
                            </Title>
                            <p>{t(section)}</p>
                        </FlexItem>
                        <Flex alignSelf={{ default: "alignSelfCenter" }}>
                            <FlexItem>
                                <Button
                                    data-testid={section}
                                    variant="tertiary"
                                    onClick={() => setShow(true)}
                                >
                                    {t(section)}
                                </Button>
                            </FlexItem>
                        </Flex>
                    </Flex>
            </div>
        </>
    );
};
